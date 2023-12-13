import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WerewolfServer implements Runnable {
    // List of people connected to the server, searched by name and holding their Player object associated with them
    ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<String, Player>();

    // Server socket
    final private ServerSocket ss;

    // Executor service that holds all the threads associated with dealing with connected users
    final private ExecutorService clientThreadPool;

    // HashMap of strings, one for each player in the actual game, where their game actions are stored if the game asks
    // them for something
    HashMap<String, String> gameActions;

    // Whether the game has actually started or not. This can change what happens when new people join, among other things
    boolean gameStart = false;

    // HashMap similar to gameActions that holds boolean values that determine if the game is waiting for the specific player
    HashMap<String, Boolean> gameWaiting;

    // HashSet of players that are playing but dead, initialized when game starts
    HashSet<Player> dead;

    // HashSet of players that are playing and not dead, initialized when game starts
    HashSet<Player> currentPlayers;

    // HashSet of cards with the correct number of duplicates that will be assigned to players, initialized on game start
    HashSet<Card> chooseCards;

    // Array of cards, one each, in the order of ranking, which determines when a certain card wakes up at night, if at all.
    // All cards are in here, including cards that may be taken out because there are more cards than players (so no player knows
    // that that card is not actually in play).
    Card[] cards;

    // An integer determining how many people the villagers will kill during each day. Usually 1, but the Troublemaker
    // can increase this value once per game to 2, which is why the separate integer is designated.
    int amountOfDayKills;

    // A master boolean flag that determines if the game is still waiting on some player's vote to kill during each day.
    boolean dayKillFlag;

    // A HashMap holding the player's votes during the day, initialized during the start of each day. The key is the player
    // who votes, and the value is the player they voted for.
    HashMap<Player, String> votes;

    // A boolean flag to let the server know everyone is voting during the day
    boolean voting;

    // The random class to give random numbers that are more random than Math.random()
    Random rand = new Random();

    // Number of death checks, incremented after someone died during a death check
    int deathCheckNum;

    // The number of werewolf cards that are in the game
    int werewolfNum;

    // Number of werewolf kills for the night
    int werewolfKills;

    // The idle times to use when a role requires input but no one alive is that role ([range, min time])
    int[] idleTimes;

    // The timers for day, werewolves, and other nights. If number is -1, then timer is off
    int[] timers;

    // main function which creates the server
    public static void main(String[] args) throws IOException {
        new WerewolfServer();
    }

    // The server constructor which sets up the server, starts the thread pool for dealing with all users connected to it,
    // and then starts its own thread of waiting for the game to start.
    WerewolfServer() throws IOException {
        ss = new ServerSocket(5555);
        System.out.println("Werewolf Server is up at " + InetAddress.getLocalHost().getHostAddress() + " on port " + ss.getLocalPort());
        clientThreadPool = Executors.newCachedThreadPool();
        new Thread(this).start();
    }

    // A method to add a person from the player HashMap when they connect.
    public void addPlayer(String playerName, Player player) {
        players.put(playerName, player);
        try {
            sendToAllPlayers("\n" + playerName + " joined the server.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // A method to remove a person from teh player HashMap when they leave or disconnect. When disconnecting, if the game
    // is ongoing, they will not be removed from the game, so they can join back. They just will miss any text sent between
    // them disconnecting and reconnecting.
    public void removePlayer(String playerName) {
        players.remove(playerName);
        try {
            sendToAllPlayers("\n" + playerName + " left the server.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // The run method that continually accepts new players and executes the thread pool for that player.
    @Override
    public void run() {
        new Thread(new GameRunner(this)).start();
        while (true) {
            try {
                Socket s = ss.accept();
                clientThreadPool.execute(new PlayerHandler(this, s));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The player handler class which is where the thread pools for each player goes into. It is constantly checking any
    // input that the player sent to the server and deals with it accordingly, whether the server was waiting for something
    // or the player asked the server a question.
    private class PlayerHandler implements Runnable {
        // The connection the player is connected through
        final private Socket clientSocket;

        // The overarching server
        final private WerewolfServer server;

        // The player's name to access other data structures with
        private String playerName;

        // The player object associated with this player to access other data structures with
        private Player player;

        // The constructor which takes only the server itself and the socket the player is connected through
        public PlayerHandler(WerewolfServer server, Socket socket) {
            this.clientSocket = socket;
            this.server = server;
        }

        // The run method that the thread pool instantly goes in after constructing. This deals with creating the player
        // object as well as constantly waiting for input from the player to deal with.
        @Override
        public void run() {
            try {
                // Setting up all important information for the player, including it's output stream, input stream,
                // player name, etc.
                String clientAddress = this.clientSocket.getInetAddress().getHostAddress();
                ObjectInputStream input = new ObjectInputStream(this.clientSocket.getInputStream());
                String joinMessage = ((String) input.readObject()).trim();
                ObjectOutputStream output = new ObjectOutputStream(this.clientSocket.getOutputStream());
                playerName = joinMessage.substring(0);

                // Checks to see if the player is a name that's not allowed
                if(playerName.equalsIgnoreCase("na") || playerName.equalsIgnoreCase("no one")) {
                    output.writeObject("Invalid name");
                    clientSocket.close();
                }

                // Checks through all already connected players and makes sure that that name is not already taken.
                for(Player player : players.values()) {
                    if(player.name.equals(playerName)) {
                        output.writeObject(playerName + " is already taken.");
                        clientSocket.close();
                    }
                }
                System.out.println("New player joined from " + clientAddress + ", " + playerName);

                // If the game has already started, it checks the players in the game and sees if there is a player with
                // that name already in there. If there is, it assigns this player that player object, because it means
                // that this player was previously disconnected.
                if(!gameStart) {
                    // If the game hasn't started yet, create the Player object like normal
                    this.player = new Player(playerName, input, output);
                } else {
                    // If it has, check all alive players and set them to that user if a match is found, making sure to
                    // update the output stream and input stream with the new one.
                    for(Player player : currentPlayers) {
                        if(player.name.equals(playerName)) {
                            this.player = player;
                            player.output = output;
                            player.input = input;
                            break;
                        }
                    }
                    // If they weren't found there, check the dead players and do the same.
                    if(this.player == null) {
                        for (Player player : dead) {
                            if (player.name.equals(playerName)) {
                                this.player = player;
                                player.output = output;
                                player.input = input;
                                break;
                            }
                        }
                    }
                    // If they still weren't found, that means they were never in the game, so create a new object for them.
                    if(this.player == null) {
                        this.player = new Player(playerName, input, output);
                    }
                }
                // Add that player to the list of players in the server
                server.addPlayer(playerName, player);


                // Start the infinite loop constantly checking and awaiting input from the player
                while(true) {
                    // Logging message showing that it is waiting for input from that player.
                    System.out.println("waiting for " + playerName);

                    // Waits at this line until the player says something
                    Object message = input.readObject();

                    // The message will always be a string, so convert it to that.
                    String temp = (String)message;

                    // Check if the game has started and if the server is waiting for this player to say something,
                    // because if so, then whatever they said is likely in response to what the server asked.
                    if(gameStart && gameWaiting.get(playerName)) {
                        // If the message starts with 'help:', it is a command to the server and not in response to the
                        // games question.
                        if(temp.startsWith("help:")) {
                            // Process out the 'help:' and get the rest of the message / command and deal with that as
                            // if the server never asked them anything.
                            String temp2 = temp.substring(temp.indexOf(":")+1).trim();
                            // Create a thread to send them back what they need, so that the server can continue to wait for
                            // new input and never miss anything.
                            new Thread(new handlePlayerCommand(temp2)).start();

                            // Logging message of the message from the player
                            System.out.println(playerName + ": " + temp2);
                            continue;
                        }
                        // If the message didn't start with 'help:', then the message was in response to the server's question,
                        // so deal with that.
                        new Thread(new handleGameAction(temp)).start();
                    } else {
                        // If the game hasn't started yet, then this is a command to the server.
                        if(temp.startsWith("help:")) {
                            // In case it starts with 'help:', take that out as it is not needed here, but included for
                            // ease of use.
                            temp = temp.substring(temp.indexOf(":")+1).trim();
                        }
                        // Create a new thread to deal with that.
                        new Thread(new handlePlayerCommand(temp)).start();

                        // Logging message of the message from the player
                        System.out.println(playerName + ": " + temp);
                    }
                }
            } catch(Exception e) {
                // If there was an error anywhere in here, it probably means that they were disconnected.
                System.out.println(e.getMessage());
                try {
                    clientSocket.close();
                    server.removePlayer(playerName);
                } catch(Exception e2) {
                    server.removePlayer(playerName);
                }
            }
        }

        // Class to deal with player commands. This is only ever called by the thread pool and has its own thread.
        public class handlePlayerCommand implements Runnable {
            String command;

            // This class only cares for the command, so set that in the constructor.
            public handlePlayerCommand(String command) {
                this.command = command;
            }

            // Process the command.
            @Override
            public void run() {
                try {
                    String result = "";
                    // If the command is help, display all commands that can be used
                    if(command.equalsIgnoreCase("commands") || command.equalsIgnoreCase("help")) {
                        result += "\n\nThe following commands can be used:\n(Also, after the server asks you for something,\nyou can type 'help:' followed by a command to access\nany of these commands)\n\n";
                        result += "'help':\t\t\tLists the possible commands\n";
                        result += "'players':\t\tLists the current alive players. Includes their votes if during voting time.\n";
                        result += "'dead':\t\t\tLists the current dead players and their cards\n";
                        result += "'cards':\t\tLists the cards that have been chosen to be in the game\n";
                        result += "'card <card name>':\tLists the description and details of the card specified by <card name>\n";
                        result += "'clients':\t\tLists the server clients (players) currently connected to the server\n";
                        result += "'order':\t\tLists the order of the cards that wake up during the nights\n\t\t\t(does not include cards that only wake up during the first night)\n";
                        result += "'win':\t\t\tLists the order in which win conditions are checked\n";
                        result += "'WhoAmI':\t\tTells you what your card is again\n";
                        result += "'needToKnow':\tTells you your card's need to know information. Not every card has some. Will also say if you're linked to someone through Cupid.\n";
                    } else if(command.equalsIgnoreCase("players")) {
                        // If the command is players, display all alive players in the game
                        if(gameStart) {
                            if(!voting) {
                                // Alive players can't be displayed if the game hasn't started yet
                                result += "\n\nThe following players are currently alive in the game:\n\n";
                                for (Player player : currentPlayers) {
                                    result += player.name;
                                    if (player.tower) {
                                        // Displays the tower next to the player if they have the tower, preventing their death the first night
                                        result += " <-- tower is currently active and thus cannot be killed this night";
                                    }
                                    result += "\n";
                                }
                            } else {
                                result += "\n\nThe following players are currently alive in the game:\n\n";
                                for(Player player : currentPlayers) {
                                    result += player.name;
                                    if(!votes.get(player).equals("")) {
                                        result += " - Voted for " + votes.get(player);
                                    } else {
                                        result += " - Hasn't voted yet";
                                    }
                                    result += "\n";
                                }
                            }
                        } else {
                            result += "\n\nThe game has not started yet, and thus there are no players\n";
                        }
                    } else if(command.equalsIgnoreCase("dead")) {
                        // Displays all dead players
                        if(gameStart) {
                            result += "\n\nThe following players are currently dead in the game:\n\n";
                            for(Player player : dead) {
                                result += player.name + ", who was a " + player.card.cardName + "\n";
                            }
                        } else {
                            result += "\n\nThe game has not started yet, and thus there are no players\n";
                        }
                    } else if(command.equalsIgnoreCase("cards")) {
                        // Displays all cards potentially in the game
                        if(gameStart) {
                            result += "\n\nThe following cards are currently in the game:\n\n";

                            // Set up the scanner for the file.
                            File file = new File("cards.txt");
                            Scanner scanner = null;
                            try {
                                scanner = new Scanner(file);
                            } catch(Exception e) {
                                System.out.println(e.getMessage());
                            }
                            // Scan every line and add that to the result string
                            while(scanner.hasNextLine()) {
                                result += scanner.nextLine();
                                result += "\n";
                            }
                            scanner.close();
                        } else {
                            result += "\n\nThe game has not started yet, and thus there are no cards\n";
                        }
                    } else if(command.length() >= 4 && command.substring(0, 4).equalsIgnoreCase("card")) {
                        // Runs the help() command for the specified card, which displays the important information about it.
                        if(gameStart) {
                            if (command.length() < 5) {
                                result += "\n\nUse this command with the name of a card, like so: 'card <card name>'\n";
                            } else {
                                // Search for the card asked about
                                String askedCard = command.substring(5);
                                boolean cardFound = false;
                                for (Card card : cards) {
                                    if (card.cardName.equalsIgnoreCase(askedCard)) {
                                        // The card was identified, so display the help() method for that card.
                                        result += "\n" + card.help() + "\n";
                                        cardFound = true;
                                    }
                                }
                                if (!cardFound) {
                                    // If the card was not found, tell the player that.
                                    result += "\nCard not found\n";
                                }
                            }
                        } else {
                            result += "\n\nThe game has not started yet, and thus there are no cards to ask about\n";
                        }
                    } else if(command.equalsIgnoreCase("clients")) {
                        // Display all people currently connected to the server
                        result += "\n\nThe following players are currently connected to the server:\n\n";
                        for(String player : players.keySet()) {
                            result += player + "\n";
                        }
                    } else if(command.equalsIgnoreCase("order")) {
                        // Display the order that cards wake up at night, excluding the first night wake-ups
                        if(gameStart) {
                            result += "\n\nThe following is the order of how the cards wake up each night (excluding first night only wakeups):\n\n";
                            for(Card card : cards) {
                                if(card.nightWakeup && !card.firstNightOnly) {
                                    result += card.cardName + "\n";
                                }
                            }
                        } else {
                            result += "\n\nThe game has not started yet, and thus there are no cards to list the order\n";
                        }
                    } else if(command.equalsIgnoreCase("win")) {
                        // Display the order that cards get checked for winning
                        if(gameStart) {
                            result += "\n\nThe following is the order of how the cards are checked for wins:\n\n";

                            // Clone the cards Array so that they can be put in order of win rank.
                            Card[] cardsForWinning = cards.clone();

                            // Sort in order of winRank
                            for(int i = 0; i < cardsForWinning.length - 1; i++) {
                                for(int j = 0; j < cardsForWinning.length - i - 1; j++) {
                                    if(cardsForWinning[j].winRank > cardsForWinning[j+1].winRank) {
                                        Card temp = cardsForWinning[j];
                                        cardsForWinning[j] = cardsForWinning[j+1];
                                        cardsForWinning[j+1] = temp;
                                    }
                                }
                            }
                            for(Card card : cardsForWinning) {
                                result += card.cardName + "\n";
                            }
                        } else {
                            result += "\n\nThe game has not started yet, and thus there are no cards to list the order\n";
                        }
                    } else if(command.equalsIgnoreCase("WhoAmI")) {
                        // Display the player's card name, which can include -> if they are a team switcher
                        if(gameStart) {
                            result += "\n\nYour card is: " + player.card.cardName;
                        } else {
                            result += "\n\nThe game hasn't started yet, so you don't have a card\n";
                        }
                    } else if(command.equalsIgnoreCase("needToKnow")) {
                        // Checks if they are linked to someone through Cupid
                        result += "\n\n";
                        for(Card card : cards) {
                            if(card instanceof CupidCard cupidCard) {
                                if(cupidCard.linked[0].equals(player)) {
                                    result += "Through Cupid, you are linked to: " + cupidCard.linked[1];
                                } else if(cupidCard.linked[1].equals(player)) {
                                    result += "Through Cupid, you are linked to: " + cupidCard.linked[0];
                                }
                                break;
                            }
                        }

                        // Display the player's card's need to know information. Not too many cards have some
                        String needToKnow = player.card.needToKnow(player);
                        if(needToKnow.equals("")) {
                            result += "\nYour card does not have any need to know information.\n";
                        } else {
                            result += "\n\n" + needToKnow + "\n";
                        }
                    } else {
                        // If the command wasn't found, tell the player that
                        result += "\n" + command + " command not found\n";
                    }

                    // Send the result of result to the player that sent the command
                    player.output.writeObject(result);
                    player.output.flush();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // This class deals with player actions in response to the server's questions. This is only ever called by the thread
        // pool and has its own thread.
        public class handleGameAction implements Runnable {
            String action;

            // This class only cares for the action, so set that in the constructor.
            public handleGameAction(String action) {
                this.action = action;
            }

            // Process the action.
            @Override
            public void run() {
                try {
                    // Set the player's action to their input.
                    gameActions.replace(playerName, action);

                    // Logging message to make it clear something was obtained from them.
                    System.out.println(playerName + "'s action: " + action);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    // This class runs the game. All game running is in this class.
    private class GameRunner implements Runnable {
        final private WerewolfServer server;

        // This game class only actually cares for the server, so set that.
        public GameRunner(WerewolfServer server) {
            this.server = server;
        }

        // The method that runs the server.
        @Override
        public void run() {
            // Gets the reader to read from command line to start the game.
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            boolean start;

            // Set default values for idle times
            idleTimes = new int[2];
            idleTimes[0] = 5000;    // Range is 5 seconds
            idleTimes[1] = 3000;    // Minimum is 3 seconds

            // Set default values for the timers
            timers = new int[3];
            timers[0] = 300000; // 5 minutes
            timers[1] = 60000; // 1 minute
            timers[2] = 30000; // 30 seconds

            // The infinite loop that goes on for as long as the server is up.
            while(true) {
                try {
                    System.out.println("We are waiting to start. Type 'start' when ready");

                    // Wait for the person running the server to type 'start' to start the game.
                    String input = br.readLine().trim();

                    // If the input wasn't 'start', continue the loop back at the start.
                    // Or maybe it was another command to edit how the server runs. Either way, check
                    if (input.equalsIgnoreCase("start")) {
                        // Input was 'start' so start the game
                        start = true;
                    } else if(input.contains("idle")) {
                        // Input was 'idle' so edit the idle time that roles do when none of them are alive
                        int max;
                        int min;
                        try {
                            // Get the times that were given
                            max = Integer.parseInt(input.substring(input.indexOf('=') + 1, input.indexOf(','))) * 1000;
                            min = Integer.parseInt(input.substring(input.indexOf(',') + 1)) * 1000;
                            if(max < min || max < 0 || min < 0) {
                                throw new IllegalArgumentException();
                            }
                            // Set first index to range and second index to min time
                            idleTimes[0] = max - min;
                            idleTimes[1] = min;
                            System.out.println("Current max: " + ((idleTimes[0]+idleTimes[1])/1000) + ", min: " + (idleTimes[1]/1000) + "\n");
                        } catch(Exception e) {
                            System.out.println("idle=<max time>,<min time>. Current max: " + ((idleTimes[0]+idleTimes[1])/1000) + ", min: " + (idleTimes[1]/1000) + "\n");
                        }
                        continue;
                    } else if(input.contains("dayTimer")) {
                        // Input was 'dayTimer' so edit the day voting timer
                        int time;
                        try {
                            // Get the time that was given
                            time = Integer.parseInt(input.substring(input.indexOf('=') + 1)) * 1000;
                            if(time < 0) {
                                throw new IllegalArgumentException();
                            }
                            // Set time
                            timers[0] = time;
                            System.out.println("Current time: " + (timers[0]/1000) + "s, " + (((double)timers[0]/1000)/60) + "min\n");
                        } catch(Exception e) {
                            System.out.println("dayTimer=<time (seconds) (0 for off)>. Current time: " + (timers[0]/1000) + "s, " + (((double)timers[0]/1000)/60) + "min\n");
                        }
                        continue;
                    } else if(input.contains("werewolfTimer")) {
                        // Input was 'werewolfTimer' so edit the day voting timer
                        int time = -1;
                        try {
                            // Get the time that was given
                            time = Integer.parseInt(input.substring(input.indexOf('=') + 1)) * 1000;
                            if(time < 0) {
                                throw new IllegalArgumentException();
                            }
                            // Set time
                            timers[1] = time;
                            System.out.println("Current time: " + (timers[1]/1000) + "s, " + (((double)timers[1]/1000)/60) + "min\n");
                        } catch(Exception e) {
                            System.out.println("dayTimer=<time (seconds) (0 for off)>. Current time: " + (timers[1]/1000) + "s, " + (((double)timers[1]/1000)/60) + "min\n");
                        }
                        continue;
                    } else if(input.contains("nightTimer")) {
                        // Input was 'nightTimer' so edit the day voting timer
                        int time = -1;
                        try {
                            // Get the time that was given
                            time = Integer.parseInt(input.substring(input.indexOf('=') + 1)) * 1000;
                            if(time < 0) {
                                throw new IllegalArgumentException();
                            }
                            // Set time
                            timers[2] = time;
                            System.out.println("Current time: " + (timers[2]/1000) + "s, " + (((double)timers[2]/1000)/60) + "min\n");
                        } catch(Exception e) {
                            System.out.println("dayTimer=<time (seconds) (0 for off)>. Current time: " + (timers[2]/1000) + "s, " + (((double)timers[2]/1000)/60) + "min\n");
                        }
                        continue;
                    } else {
                        System.out.println("'start', 'idle=<max time (seconds)>,<min time (seconds)>', 'dayTimer=<time (seconds) (0 for off)>'");
                        System.out.println("'werewolfTimer=<time (seconds) (0 for off)>', 'nightTimer=<time (seconds) (0 for off)>'");
                        continue;
                    }

                    // If the server runner said start, and there are at least 5 players (Werewolf needs at minimum 5 players),
                    // the game can start.
                    if (start && server.players.size() >= 5) {
                        System.out.println("Starting");

                        // Set all players in the server to alive.
                        for(Player player : players.values()) {
                            player.dead = false;
                        }

                        // Get all the players in the game situated by setting them all as in the alive HashSet and getting
                        // their gameActions and gameWaiting data structures ready.
                        gameActions = new HashMap<String, String>();
                        gameWaiting = new HashMap<String, Boolean>();
                        currentPlayers = new HashSet<Player>();

                        // Initialize the dead HashSet.
                        dead = new HashSet<Player>();

                        int i = 0;
                        for(String name : server.players.keySet()) {
                            currentPlayers.add(server.players.get(name));
                            gameActions.put(name, "");
                            gameWaiting.put(name, Boolean.FALSE);
                            i++;
                        }

                        // Read the cards that will be played with from the cards.txt file.
                        readCards();
                        boolean hasWerewolves = false;
                        for(Card card : cards) {
                            if(card.cardName.equals("Werewolf") || card.cardName.equals("Dire Wolf") || card.cardName.equals("Wolf Man") || card.cardName.equals("Wolf Cub")) {
                                hasWerewolves = true;
                                break;
                            }
                        }
                        if(!hasWerewolves) {
                            System.out.println("There are no werewolf cards.");
                            continue;
                        }

                        // Set the werewolf kills to 1
                        werewolfKills = 1;


                        // The preCheck stuff (sort for preCheck order, then preCheck)
                        Card[] preCheckCards = preCheckSort(cards.clone());
                        try {
                            for (Card card : preCheckCards) {
                                card.preCheck();
                            }
                        } catch(IllegalArgumentException e) {
                            System.out.println(e.getMessage());
                            continue;
                        }

                        // Set the start flag to true for the rest of the program.
                        gameStart = true;

                        // Make it clear to all players where the new game chat starts
                        sendToAllPlayers("================================");
                        sendToAllPlayers("New Game!\n\n");

                        // Check how many werewolf cards are being played
                        werewolfNum = 0;
                        for(Card card : chooseCards) {
                            if(card.cardName.equals("Werewolf") || card.cardName.equals("Dire Wolf") ||
                            card.cardName.equals("Wolf Man") || card.cardName.equals("Wolf Cub")) {
                                werewolfNum++;
                            }
                        }

                        // Set death check num
                        deathCheckNum = 1;

                        rand = new Random(System.currentTimeMillis());

                        // If the amount of cards is equal to the amount of players, just assign each player a random card,
                        // with all cards being used once each (there can be duplicates of actual cards, which is stated
                        // in the cards.txt file).
                        if(chooseCards.size() == currentPlayers.size()) {
                            // Create an array for the cards so that a random value can be obtained for the card and
                            // assigned to a player.
                            Object[] tempCards = chooseCards.toArray();

                            // A copy of the chooseCards HashSet is created so that cards that are already used can be removed.
                            HashSet<Card> tempCards2 = (HashSet<Card>) chooseCards.clone();

                            // Run through each player in the game and give them a random card.
                            for (Player player : currentPlayers) {
                                int random = rand.nextInt(tempCards.length);
                                player.card = (Card) tempCards[random];
                                tempCards2.remove(player.card);
                                tempCards = tempCards2.toArray();
                            }
                        } else if(chooseCards.size() > currentPlayers.size()) {
                            // If there are more cards than players, remove extra cards at random (making sure all specified
                            // werewolves are in the game).

                            // The werewolf cards.
                            HashSet<Card> werewolves = new HashSet<Card>();

                            // Just like in the section above where the card amount is equal to the player amount, make
                            // a clone of the chooseCards HashSet.
                            HashSet<Card> chooseCardsClone = (HashSet<Card>) chooseCards.clone();

                            // For each card in chooseCards, find all that are werewolves of any kind (Werewolf, Wolf Man, Dire Wolf,
                            // Wolf Cub), and remove them so that extra cards can be removed and none of them are werewolves.
                            for(Card card : chooseCards) {
                                if(card.cardName.equals("Werewolf") || card.cardName.equals("Wolf Man") || card.cardName.equals("Dire Wolf") || card.cardName.equals("Wolf Cub")) {
                                    // Temporarily remove the werewolf cards.
                                    werewolves.add(card);
                                    chooseCardsClone.remove(card);
                                }
                            }

                            // Remove the amount of extra cards from the remaining cards (the non-werewolf cards), so that
                            // there are the exact amount of cards (when werewolves are added back) as there are players.
                            int removeCardAmount = chooseCards.size() - currentPlayers.size();

                            // Run through the amount of cards that need to be removed and randomly remove that amount of cards.
                            for(i = 0; i < removeCardAmount; i++) {
                                int random = rand.nextInt(chooseCardsClone.size());
                                chooseCardsClone.remove(chooseCardsClone.toArray()[random]);
                            }

                            // Add the werewolf cards back in to the chooseCards clone so that all of the remaining cards
                            // can be given out to all players.
                            for(Card card : werewolves) {
                                chooseCardsClone.add(card);
                            }

                            // Assign the cards just like the if statement above all of this.
                            Object[] tempCards = chooseCardsClone.toArray();
                            HashSet<Card> tempCards2 = (HashSet<Card>) chooseCardsClone.clone();
                            for (Player player : currentPlayers) {
                                int random = rand.nextInt(tempCards.length);
                                player.card = (Card) tempCards[random];
                                tempCards2.remove(player.card);
                                tempCards = tempCards2.toArray();
                            }


                            // If there are fewer cards than players, don't continue the game and let the server moderator know
                            // they need to add more cards.
                        } else {
                            System.out.println("\nThere aren't enough cards for the amount of players.");
                            start = false;
                            continue;
                        }

                        // Tell each player their card.
                        for(Player player : currentPlayers) {
                            player.output.writeObject("Your card: " + player.card.cardName);
                        }

                        // Tell everyone to close their eyes
                        sendToAllPlayers("Close your eyes!");
                        Thread.sleep(3000);

                        // Set the amount of day kills to 1. The Troublemaker can decide to increase this to 2 only once
                        // every game, but it can be the first night if they so choose.
                        amountOfDayKills = 1;

                        // Run the loop for first night wakeup. There are many cards that have special first night wakeup
                        // and there are also many that have each night the same as the first night. There are also some
                        // that don't wake up at all every night. All of that is taken care of in the specific card.
                        // If cards do the same thing the first night as they do every other night, their firstNightWakeup()
                        // method will call their nightWakeup() method. If they don't do anything at night, they will
                        // immediately return. The cards are already in the order of night wakeup when the cards array was
                        // chosen, so it will run through them linearly.
                        for(Card card : cards) {
                            if(card.nightWakeup) {
                                card.firstNightWakeup();
                                // Have the game thread sleep for 3 seconds after each night wakeup to ensure that threads
                                // that should die now that their night wakeup is over, actually die after doing all they
                                // need to.
                                Thread.sleep(3000);
                                // Make sure that the server is no longer waiting for any player.
                                stopWaiting();
                            }
                        }
                        werewolfKills = 1;

                        // The night is over, so wake up everyone so that they can see who died and they can determine who to kill for the day.
                        sendToAllPlayers("\nNow everyone open your eyes.");

                        // A flag to make sure someone has died
                        boolean hasDied = false;

                        // Run through the alive players and check who was set to dead after the entire night, and add them to the
                        // dead HashSet.
                        for(Player player : currentPlayers) {
                            if(player.dead) {
                                dead.add(player);
                                hasDied = true;
                            }
                        }

                        // If no one died, tell everyone that no one died
                        if(!hasDied) {
                            sendToAllPlayers("\nNo one died tonight!\n");
                        }

                        // For all players in the dead HashSet, because it was just created, all of those people are newly dead.
                        for(Player player : dead) {
                            // Tell all players who have been killed and what their cards were.
                            sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!\n");
                            // Remove them from the alive players HashSet.
                            currentPlayers.remove(player);
                            // Because they died on the very first night and did not get to play, set their tower to true, meaning
                            // they cannot die on the first night the next game (so they actually get to play).
                            player.tower = true;

                            // Tell that player they are dead
                            player.output.writeObject("!!!!!YOU DIED!!!!!");
                        }

                        // If the card has something it needs to check after all the deaths, like linked people, do it now
                        // Do it the number of times that people died during a death check + 1
                        for(int k = 0; k < deathCheckNum; k++) {
                            for (Card card : cards) {
                                card.checkAfterDeaths();
                            }
                        }
                        deathCheckNum = 1;

                        // Check if after the first night, someone already won. This is unlikely, but could happen in
                        // the case of the Tanner, who wins if they die.
                        Card won = checkWins();
                        if(won != null) {
                            // Tell everyone which team won.
                            sendToAllPlayers("The " + won.team + " team won!");

                            // Make a HashSet of players that were on that team.
                            HashSet<Player> winningPlayers = new HashSet<Player>();
                            for(Player player : currentPlayers) {
                                if(player.card.team.equals(won.team)) {
                                    // Add every player that won to that HashSet.
                                    winningPlayers.add(player);
                                }
                            }

                            // Add all dead players of that HashSet too, since they still technically win if their team does.
                            for(Player player : dead) {
                                if(player.card.team.equals(won.team)) {
                                    winningPlayers.add(player);
                                }
                            }

                            // Tell all players who was on that winning team.
                            sendToAllPlayers("Winning players: " + winningPlayers);
                            continue;
                        }

                        // Make sure that no player in the alive HashSet has a tower set since they survived. This is the only
                        // place they get cleared between games so that the state of their tower from last game is preserved.
                        for(Player player : currentPlayers) {
                            player.tower = false;
                        }

                        // Run the infinite loop for the game. Every day, then every night at the end, until someone won.
                        while(true) {
                            sendToAllPlayers("Now, you all need to discuss and pick a person each that you will kill.");
                            sendToAllPlayers("Type 'na', 'NA', or 'no one' to vote for no one.");
                            sendToAllPlayers("The number of people you must choose to kill is: " + amountOfDayKills + "\n");

                            // Loop through the amount of kills there are that day
                            for(int j = 0; j < amountOfDayKills; j++) {

                                // Create the HashMap that holds all alive players votes.
                                votes = new HashMap<Player, String>();
                                for (Player player : server.currentPlayers) {
                                    votes.put(player, "");
                                }
                                // Tell the server it is waiting for all alive players' actions.
                                for(Player player : currentPlayers) {
                                    gameWaiting.replace(player.name, Boolean.TRUE);
                                }

                                // Tell the players what kill this is
                                for(Player player : currentPlayers) {
                                    player.output.writeObject("\nWho is your kill #" + (j + 1) + "?");
                                }

                                // Set a flag so the server knows everyone is voting
                                voting = true;

                                // Set a flag to false signifying that the day isn't over. This only gets set to true
                                // once all alive players have chosen a valid player to kill (valid as in they are still alive).
                                dayKillFlag = false;

                                // Create a thread that continuously sends a player's valid vote to all other players.
                                new Thread(this::sendAllVotes).start();

                                // Check if the day timer is on, and if so, create the thread to keep the time
                                Thread votingThread = null;
                                if(timers[0] > 0) {
                                    int time;
                                    if(j > 0) {
                                        time = 10000;
                                    } else {
                                        time = timers[0];
                                    }

                                    votingThread = new Thread(() -> server.timerHelper(time));
                                    votingThread.start();
                                }

                                // While the flag is false, run through this code.
                                while (!dayKillFlag) {
                                    // Set a temporary flag that checks if all players have chosen someone.
                                    boolean good = true;

                                    // Create a count map that is used to find the most popular player to kill.
                                    HashMap<String, Integer> count = new HashMap<String, Integer>();
                                    for (Player player : server.currentPlayers) {
                                        count.put(player.name, 0);
                                    }

                                    // Put the option for no vote in there
                                    count.put("no one", 0);

                                    // Step through all alive players and check to see if they voted. Additionally, talley
                                    // the player they voted for.
                                    for (String player : votes.values()) {
                                        if (!player.equals("")) {
                                            count.replace(player, count.get(player) + 1);
                                        } else {
                                            // If they did not vote, then that means the server can't continue so just quit
                                            // and try again.
                                            good = false;
                                            break;
                                        }
                                    }
                                    // A player didn't vote yet, so restart this loop.
                                    if (!good) {
                                        continue;
                                    }

                                    // If it got here, that means that all players have voted. Set the flag to true.
                                    dayKillFlag = true;

                                    // Stop the timer if it's on
                                    if(timers[0] > 0) {
                                        votingThread.interrupt();
                                    }
                                    // Wait 3 seconds to allow the threads created to help get player votes to end.
                                    Thread.sleep(3000);
                                    // Make sure the server is no longer waiting for any player.
                                    stopWaiting();

                                    HashSet<Player> deadCopy = (HashSet<Player>) dead.clone();

                                    // Loop through the votes for the amount of kills necessary and get the highest player.
                                    int highest = -1;
                                    String dead2 = "";
                                    for (String player : count.keySet()) {
                                        if (count.get(player) > highest) {
                                            highest = count.get(player);
                                            dead2 = player;
                                        }
                                    }
                                    // That player who was chosen is set to dead.
                                    if(!dead2.equals("no one")) {
                                        Player dead3 = null;
                                        for (Player player : currentPlayers) {
                                            if (player.name.equals(dead2)) {
                                                dead3 = player;
                                                break;
                                            }
                                        }
                                        dead3.dead = true;
                                        count.remove(dead2);

                                        try {
                                            server.sendToAllPlayers("Voted dead player #" + (j + 1) + ": " + dead3.name + "\n");
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                        }
                                    } else {
                                        try {
                                            server.sendToAllPlayers("Voted dead player #" + (j + 1) + ": No one was chosen!\n");
                                        } catch (Exception e) {
                                            System.out.println(e.getMessage());
                                        }
                                    }
                                }
                            }

                            // Unset the voting flag
                            voting = false;

                            // Tell everyone that voting is over
                            try {
                                sendToAllPlayers("\n\nVoting is now over.\n\n");
                            } catch(Exception e) {
                                System.out.println(e.getMessage());
                            }

                            // A temporary dead HashSet is created to log the newly dead players.
                            HashSet<Player> deadCopy = new HashSet<Player>();
                            // Loops through all supposedly alive players and checks to see which are dead.
                            for(Player player : currentPlayers) {
                                if(player.dead) {
                                    // Adds them to the dead HashSet and the dead copy HashSet.
                                    dead.add(player);
                                    deadCopy.add(player);
                                }
                            }

                            // Loops through all in the dead copy HashSet and alerts every one of their death and what card they were.
                            for(Player player : deadCopy) {
                                // If the player is not a Prince, continue as normal
                                if(!player.card.cardName.contains("Prince")) {
                                    sendToAllPlayers("\n" + player.name + " has been chosen to be killed!\nThey were " + player.card.cardName + "!\n");
                                    currentPlayers.remove(player);

                                    // Tell that player they are dead
                                    player.output.writeObject("!!!!!YOU DIED!!!!!");
                                } else {
                                    // If the player is a Prince, get the Prince card the server uses
                                    for(Card card : server.cards) {
                                        if(card instanceof PrinceCard princeCard) {
                                            // Check if their ability has already been used
                                            if(princeCard.abilityUsed) {
                                                // If it has, they die like normal
                                                sendToAllPlayers("\n" + player.name + " has been chosen to be killed!\nThey were " + player.card.cardName + "!\n");
                                                currentPlayers.remove(player);

                                                // Tell that player they are dead
                                                player.output.writeObject("!!!!!YOU DIED!!!!!");
                                            } else {
                                                // If it hasn't, the players are notified that they are the Prince and are not killed, this time
                                                sendToAllPlayers("\n" + player.name + " is the Prince! Therefore they are not killed... this time.\n");
                                                princeCard.abilityUsed = true;
                                                player.dead = false;
                                                dead.remove(player);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }

                            // If the card has something it needs to check after all the deaths, like linked people, do it now
                            // Do it the amount of times that someone during the death checks died + 1
                            for(int k = 0; k < deathCheckNum; k++) {
                                for (Card card : cards) {
                                    card.checkAfterDeaths();
                                }
                            }
                            deathCheckNum = 1;

                            // Resets the amount of day kills to 1. May have already been 1, may have been changed by another card
                            // during the night.
                            amountOfDayKills = 1;

                            // Run through all cards to check if any team won, just like above.
                            won = checkWins();
                            if(won != null) {
                                sendToAllPlayers("The " + won.team + " team won!");
                                HashSet<Player> winningPlayers = new HashSet<Player>();
                                for(Player player : currentPlayers) {
                                    if(player.card.team.equals(won.team)) {
                                        winningPlayers.add(player);
                                    }
                                }
                                for(Player player : dead) {
                                    if(player.card.team.equals(won.team)) {
                                        winningPlayers.add(player);
                                    }
                                }
                                sendToAllPlayers("Winning players: " + winningPlayers);
                                break;
                            }

                            // Now for the night again. This is the normal night, not the first night.
                            sendToAllPlayers("Now for the night. Everyone close your eyes.\n");
                            Thread.sleep(3000);
                            for(Card card : cards) {
                                if(card.nightWakeup) {
                                    // Runs through the nights same as the first night, except it calls the normal
                                    // night method rather than the first night method.
                                    card.nightWakeup();
                                    // Waits 3 seconds again to make sure all other threads finish what they were doing.
                                    Thread.sleep(3000);
                                    // Makes sure the game isn't waiting for anyone anymore.
                                    stopWaiting();
                                }
                            }
                            werewolfKills = 1;

                            // The night is over, so wake up everyone so that they can see who died, and they can determine who to kill for the day.
                            sendToAllPlayers("\nNow everyone open your eyes.");

                            // Resets the hasDied flag to check if anyone has died this night
                            hasDied = false;

                            // Just like above, it creates a dead copy and checks to see who is newly dead.
                            deadCopy = new HashSet<Player>();
                            for(Player player : currentPlayers) {
                                if(player.dead) {
                                    dead.add(player);
                                    deadCopy.add(player);
                                    hasDied = true;
                                }
                            }

                            // If no one died, tell everyone that
                            if(!hasDied) {
                                sendToAllPlayers("\nNo one died tonight!\n");
                            }

                            // Runs through the newly dead and alerts everyone of their death.
                            for(Player player : deadCopy) {
                                sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!\n");
                                currentPlayers.remove(player);

                                // Tell that player they are dead
                                player.output.writeObject("!!!!!YOU DIED!!!!!");
                            }

                            // If the card has something it needs to check after all the deaths, like linked people, do it now
                            // Do it the amount of times that someone during the death checks died + 1
                            for(int k = 0; k < deathCheckNum; k++) {
                                for (Card card : cards) {
                                    card.checkAfterDeaths();
                                }
                            }
                            deathCheckNum = 1;


                            // Just like above, checks to see if anyone won as a result of the night.
                            won = checkWins();
                            if(won != null) {
                                sendToAllPlayers("The " + won.team + " team won!");
                                HashSet<Player> winningPlayers = new HashSet<Player>();
                                for(Player player : currentPlayers) {
                                    if(player.card.team.equals(won.team)) {
                                        winningPlayers.add(player);
                                    }
                                }
                                for(Player player : dead) {
                                    if(player.card.team.equals(won.team)) {
                                        winningPlayers.add(player);
                                    }
                                }
                                sendToAllPlayers("Winning players: " + winningPlayers);
                                break;
                            }
                        }

                        // If it finally left that loop, that means the game is over, so set the game to not be going.
                        gameStart = false;
                        // Clear all game related maps and sets.
                        gameActions.clear();
                        gameWaiting.clear();
                        currentPlayers.clear();
                    } else if(server.players.size() < 5) {
                        System.out.println("We need more people. Need at least 5");
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // Check if any cards won.
        private Card checkWins() {
            // Clone the cards Array so that they can be put in order of win rank.
            Card[] cardsForWinning = cards.clone();

            // Sort in order of winRank
            for(int i = 0; i < cardsForWinning.length - 1; i++) {
                for(int j = 0; j < cardsForWinning.length - i - 1; j++) {
                    if(cardsForWinning[j].winRank > cardsForWinning[j+1].winRank) {
                        Card temp = cardsForWinning[j];
                        cardsForWinning[j] = cardsForWinning[j+1];
                        cardsForWinning[j+1] = temp;
                    }
                }
            }

            // Check through all cards in the order of win rank to see who won (call their won method).
            for(Card card : cardsForWinning) {
                System.out.println("Checking win of " + card.cardName);
                if(card.won()) {
                    // If a card won, return it.
                    return card;
                }
            }
            // If no card won, return null.
            return null;
        }

        // A method to get all votes for all alive players.
        private void sendAllVotes() {
            String[] possibilities = new String[currentPlayers.size()];
            int i = 0;
            for(Player player : currentPlayers) {
                if(!player.dead) {
                    possibilities[i] = player.name;
                    i++;
                }
            }

            // Run through the infinite loop until everyone has voted for a person
            while(!dayKillFlag) {
                // While the server is still waiting for everyone to vote.
                for(Player player : currentPlayers) {
                    // If the server found that all players voted NOW, during each for execution, quit out of the loop.
                    if(dayKillFlag) {
                        break;
                    }
                    // Make sure that the vote of a player is a valid player that is alive (can be themselves if they want). Also makes sure it doesn't send to all if
                    // it already sent to all.
                    if(!server.gameActions.get(player.name).equals("") && (Arrays.asList(possibilities).contains(gameActions.get(player.name)) ||
                            gameActions.get(player.name).equalsIgnoreCase("NA") || gameActions.get(player.name).equalsIgnoreCase("no one"))) {
                        try {
                            // Send their vote to all players.
                            if(!gameActions.get(player.name).equalsIgnoreCase("NA") && !gameActions.get(player.name).equalsIgnoreCase("no one")) {
                                sendToAllPlayers(player.name + " voted: " + server.gameActions.get(player.name));
                            } else {
                                sendToAllPlayers(player.name + " voted for no one");
                            }
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        // Sets the last vote the player made to this vote. Also logs their vote.
                        if(gameActions.get(player.name).equalsIgnoreCase("na")) {
                            votes.replace(player, "no one");
                        } else {
                            votes.replace(player, gameActions.get(player.name));
                        }
                        gameActions.replace(player.name, "");
                    } else if(!gameActions.get(player.name).equals("") && !Arrays.asList(possibilities).contains(gameActions.get(player.name))) {
                        // If the player's vote wasn't a valid player.
                        try {
                            player.output.writeObject("Not a valid player");
                            // Replace the HashMap for the player that the server checks to see if anything was inputted.
                            server.gameActions.replace(player.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }

        // Read all cards in the cards.txt file.
        private void readCards() throws FileNotFoundException {
            // Set up the scanner for the file.
            File file = new File("cards.txt");
            Scanner scanner = new Scanner(file);

            // Create multiple temp data structures for the cards.
            HashSet<Card> temp = new HashSet<Card>();
            HashSet<Card> temp2 = new HashSet<Card>();
            // Scan every line
            while(scanner.hasNextLine()) {
                String card = scanner.nextLine();
                String cardName;
                // If the card is in the file, that means there is at least 1 of them.
                int cardAmount = 1;
                // Cards can be in the form of 'werewolves' or 'werewolves x4', the second one meaning there are 4
                // werewolves. Grab the number after the x if that exists.
                if(card.contains(" x")) {
                    // Set the card name as everything before the ' x'.
                    cardName = card.substring(0, card.indexOf("x") - 1);
                    // Get the number of cards, which is directly after the 'x'.
                    cardAmount = Integer.parseInt(card.substring(card.indexOf("x")+1));
                } else {
                    // If it wasn't in the form of ' x4' or another number, everything the scanner got is the
                    // card's name.
                    cardName = card;
                }

                // Make a flag for making sure that the cards array only has 1 of every card and the chooseCards
                // HashSet has the amount of cards that the text file specified.
                boolean doneOnce = false;
                // Go for the amount of cards specified (1 is default).
                for(int i = 0; i < cardAmount; i++) {
                    Card tempCard = null;
                    Card tempCard2 = null;
                    if (cardName.equalsIgnoreCase("villager") || cardName.equalsIgnoreCase("villagers")) {
                        // If the card is a plain villager, create a new villager card object.
                        tempCard = new VillagerCard(server);
                        tempCard2 = new VillagerCard(server);
                    } else if (cardName.equalsIgnoreCase("werewolf") || cardName.equalsIgnoreCase("werewolves")) {
                        // If the card is a plain werewolf card.
                        tempCard = new WerewolfCard(server);
                        tempCard2 = new WerewolfCard(server);
                    } else if (cardName.equalsIgnoreCase("tanner")) {
                        // If the card is a tanner card.
                        tempCard = new TannerCard(server);
                        tempCard2 = new TannerCard(server);
                    } else if (cardName.equalsIgnoreCase("bodyguard")) {
                        // If the card is a bodyguard card.
                        tempCard = new BodyguardCard(server);
                        tempCard2 = new BodyguardCard(server);
                    } else if (cardName.equalsIgnoreCase("troublemaker")) {
                        // If the card is a troublemaker card.
                        tempCard = new TroublemakerCard(server);
                        tempCard2 = new TroublemakerCard(server);
                    } else if(cardName.equalsIgnoreCase("seer")) {
                        // If the card is a seer card.
                        tempCard = new SeerCard(server);
                        tempCard2 = new SeerCard(server);
                    } else if(cardName.equalsIgnoreCase("cupid")) {
                        // If the card is a cupid card.
                        tempCard = new CupidCard(server);
                        tempCard2 = new CupidCard(server);
                    } else if(cardName.equalsIgnoreCase("minion")) {
                        // If the card is a minion card.
                        tempCard = new MinionCard(server);
                        tempCard2 = new MinionCard(server);
                    } else if(cardName.equalsIgnoreCase("drunk")) {
                        // If the card is a drunk card.
                        tempCard = new DrunkCard(server);
                        tempCard2 = new DrunkCard(server);
                    } else if(cardName.equalsIgnoreCase("doppelganger")) {
                        // If the card is a doppelganger card.
                        tempCard = new DoppelgangerCard(server);
                        tempCard2 = new DoppelgangerCard(server);
                    } else if(cardName.equalsIgnoreCase("cursed")) {
                        // If the card is a cursed card.
                        tempCard = new CursedCard(server);
                        tempCard2 = new CursedCard(server);
                    } else if(cardName.equalsIgnoreCase("dire wolf")) {
                        // If the card is a dire wolf card.
                        tempCard = new DireWolfCard(server);
                        tempCard2 = new DireWolfCard(server);
                    } else if(cardName.equalsIgnoreCase("hoodlum")) {
                        // If the card is a hoodlum card.
                        tempCard = new HoodlumCard(server);
                        tempCard2 = new HoodlumCard(server);
                    } else if(cardName.equalsIgnoreCase("wolf man")) {
                        // If the card is a wolf man card.
                        tempCard = new WolfManCard(server);
                        tempCard2 = new WolfManCard(server);
                    } else if(cardName.equalsIgnoreCase("wolf cub")) {
                        // If the card is a wolf cub card.
                        tempCard = new WolfCubCard(server);
                        tempCard2 = new WolfCubCard(server);
                    } else if(cardName.equalsIgnoreCase("lycan")) {
                        // If the card is a lycan card.
                        tempCard = new LycanCard(server);
                        tempCard2 = new LycanCard(server);
                    } else if(cardName.equalsIgnoreCase("hunter")) {
                        // If the card is a hunter card.
                        tempCard = new HunterCard(server);
                        tempCard2 = new HunterCard(server);
                    } else if(cardName.equalsIgnoreCase("mason")) {
                        // If the card is a mason card.
                        tempCard = new MasonCard(server);
                        tempCard2 = new MasonCard(server);
                    } else if(cardName.equalsIgnoreCase("diseased")) {
                        // If the card is a diseased card.
                        tempCard = new DiseasedCard(server);
                        tempCard2 = new DiseasedCard(server);
                    } else if(cardName.equalsIgnoreCase("prince")) {
                        // If the card is a prince card.
                        tempCard = new PrinceCard(server);
                        tempCard2 = new PrinceCard(server);
                    } else if(cardName.equalsIgnoreCase("apprentice seer")) {
                        // If the card is an apprentice seer card.
                        tempCard = new ApprenticeSeerCard(server);
                        tempCard2 = new ApprenticeSeerCard(server);
                    } else {
                        // If the card is not recognized, throw an error to jump out of here.
                        System.out.println("Card not recognized.");
                        gameStart = false;
                        throw new IllegalArgumentException();
                    }

                    // Put the newly created card in the temp HashMap.
                    temp.add(tempCard);
                    // If this is the first iteration of this card, add it to the temp2 HashSet as well.
                    if(!doneOnce) {
                        temp2.add(tempCard2);
                        // Make sure to set the done once flag to true.
                        doneOnce = true;
                    }
                }
            }
            // Once the program gets here, all lines have been read in the cards.txt file, and thus all cards are grabbed.
            scanner.close();

            // Initialize the chooseCards HashSet for players to be assigned cards.
            server.chooseCards = new HashSet<Card>();
            int i = 0;
            // Add all cards that were in the temp HashMap.
            for(Card card : temp) {
                server.chooseCards.add(card);
                i++;
            }

            // Initialize the cards Array.
            cards = new Card[temp2.size()];
            i = 0;
            // Add all cards that were in the temp2 HashSet.
            for(Card card : temp2) {
                cards[i] = card;
                i++;
            }

            // Sort in order of rank for night wakeup.
            for(i = 0; i < cards.length - 1; i++) {
                for(int j = 0; j < cards.length - i - 1; j++) {
                    if(cards[j].ranking > cards[j+1].ranking) {
                        Card temp3 = cards[j];
                        cards[j] = cards[j + 1];
                        cards[j + 1] = temp3;
                    }
                }
            }
        }
    }

    // A helper method to make sure that the server is no longer waiting for any players when needed.
    public void stopWaiting() {
        for (Player player : currentPlayers) {
            gameWaiting.replace(player.name, Boolean.FALSE);
            gameActions.replace(player.name, "");
        }
    }

    // A helper method to send a message to all players in the server, so that even dead can follow along in the game.
    public void sendToAllPlayers(String message) throws IOException {
        for(Player player : players.values()) {
            player.output.writeObject(message);
        }
    }

    // A helper method used by other cards, including the Werewolf card, to check if a player is a type of werewolf
    public boolean checkWerewolf(Player player) {
        return player.card.cardName.contains("Werewolf") || player.card.cardName.contains("Dire Wolf") ||
                player.card.cardName.contains("Wolf Cub") || player.card.cardName.contains("Wolf Man");
    }

    // A helper method to sort for preCheck order
    public Card[] preCheckSort(Card[] checkCards) {
        // Sort in order of rank for night wakeup.
        for(int i = 0; i < checkCards.length - 1; i++) {
            for(int j = 0; j < checkCards.length - i - 1; j++) {
                if(checkCards[j].preCheckRank > checkCards[j+1].preCheckRank) {
                    Card temp3 = checkCards[j];
                    checkCards[j] = checkCards[j + 1];
                    checkCards[j + 1] = temp3;
                }
            }
        }
        return checkCards;
    }

    // The helper method to run the timer for day voting. It will run the timer for the given time, and then if it returns
    // (the only way it doesn't is if the thread was interrupted), it will set all the player's votes who haven't voted
    // to no one
    public synchronized void timerHelper(int time) {
        // Get all the players so everyone is aware of how much time is left
        Player[] players2 = new Player[players.size()];
        int i = 0;
        for(Player player : players.values()) {
            players2[i] = player;
            i++;
        }

        // Call the timer method for the alive players and the time given
        timer(time, players2);

        // If it gets here, that means the players ran out of time, so set all who haven't voted to vote for no one
        for(Player player : votes.keySet()) {
            if(votes.get(player).equals("")) {
                gameActions.replace(player.name, "no one");
                try {
                    Thread.sleep(500);
                } catch(Exception e) {
                    System.out.println();
                }
            }
        }
    }

    // The timer for everything. Time is the amount of time, players are the players to tell when it's about to end
    public synchronized void timer(int time, Player[] players) {
        // Check line 894 and 936 <-----------------------------------------------------------------------------------------------------------------------
        if(time < 0) {
            throw new IllegalArgumentException("Timer must be above 0");
        }
        try {
            if (time <= 10000) {
                for(Player player : players) {
                    player.output.writeObject("You have " + (time/1000) + " seconds to vote.");
                }
                Thread.sleep(time);
            } else {
                Thread.sleep(time - 10000);
                for(Player player : players) {
                    player.output.writeObject("You have 10 seconds left.");
                }
                Thread.sleep(10000);
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}