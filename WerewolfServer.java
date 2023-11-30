import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WerewolfServer implements Runnable {
    ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<String, Player>();
    private ServerSocket ss;
    private ExecutorService clientThreadPool;
    HashMap<String, String> gameActions;
    boolean gameStart = false;
    HashMap<String, Boolean> gameWaiting;
    HashSet<Player> dead;
    HashSet<Player> currentPlayers;
    HashSet<Card> chooseCards;
    Card[] cards;

    int amountOfDayKills;
    boolean dayKillFlag;
    HashMap<Player, Player> votes;

    public static void main(String[] args) throws IOException {
        new WerewolfServer();
    }

    WerewolfServer() throws IOException {
        ss = new ServerSocket(5555);
        System.out.println("Werewolf Server is up at " + InetAddress.getLocalHost().getHostAddress() + " on port " + ss.getLocalPort());
        clientThreadPool = Executors.newCachedThreadPool();
        new Thread(this).start();
    }

    public void addPlayer(String playerName, Player player) {
        players.put(playerName, player);
    }

    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

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

    private class PlayerHandler implements Runnable {
        private Socket clientSocket;
        private WerewolfServer server;
        private String playerName;
        private Player player;

        public PlayerHandler(WerewolfServer server, Socket socket) {
            this.clientSocket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                String clientAddress = this.clientSocket.getInetAddress().getHostAddress();
                ObjectInputStream input = new ObjectInputStream(this.clientSocket.getInputStream());
                String joinMessage = ((String) input.readObject()).trim();
                ObjectOutputStream output = new ObjectOutputStream(this.clientSocket.getOutputStream());
                playerName = joinMessage.substring(0);
                for(Player player : players.values()) {
                    if(player.name.equals(playerName)) {
                        output.writeObject(playerName + " is already taken.");
                        clientSocket.close();
                    }
                }
                System.out.println("New player joined from " + clientAddress + ", " + playerName);

                if(!gameStart) {
                    this.player = new Player(playerName, input, output);
                } else {
                    for(Player player : currentPlayers) {
                        if(player.name.equals(playerName)) {
                            this.player = player;
                            player.output = output;
                            player.input = input;
                            break;
                        }
                    }
                }
                server.addPlayer(playerName, player);


                while(true) {
                    System.out.println("waiting for " + playerName);
                    Object message = input.readObject();
                    String temp = (String)message;
                    System.out.println(playerName + ": " + temp);
                    if(gameStart && gameWaiting.get(playerName)) {
                        if(temp.startsWith("help")) {
                            String temp2 = temp.substring(temp.indexOf(":")+1).trim();
                            new Thread(new handlePlayerCommand(temp2)).start();
                            continue;
                        }
                        new Thread(new handleGameAction(temp)).start();
                    } else {
                        if(temp.startsWith("help:")) {
                            temp = temp.substring(temp.indexOf(":")+1).trim();
                        }
                        new Thread(new handlePlayerCommand(temp)).start();
                    }
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
                server.removePlayer(playerName);
            }
        }

        public class handlePlayerCommand implements Runnable {
            String command;

            public handlePlayerCommand(String command) {
                this.command = command;
            }

            @Override
            public void run() {
                try {
                    System.out.println("We got the message: " + command);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        public class handleGameAction implements Runnable {
            String action;

            public handleGameAction(String action) {
                this.action = action;
            }

            @Override
            public void run() {
                try {
                    gameActions.replace(playerName, action);
                    System.out.println("We got the action: " + action);
                    gameWaiting.replace(playerName, Boolean.FALSE);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private class GameRunner implements Runnable {
        private WerewolfServer server;

        public GameRunner(WerewolfServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            boolean start = false;
            while(true) {
                try {
                    System.out.println("We are waiting to start. Type 'start' when ready");
                    String input = br.readLine().trim();
                    if (input.equalsIgnoreCase("start")) {
                        start = true;
                    }

                    if (start && server.players.size() >= 5) {
                        System.out.println("Starting");
                        gameStart = true;

                        // Get all the players in the game situated
                        gameActions = new HashMap<String, String>();
                        gameWaiting = new HashMap<String, Boolean>();
                        currentPlayers = new HashSet<Player>();
                        int i = 0;
                        for(String name : server.players.keySet()) {
                            currentPlayers.add(server.players.get(name));
                            gameActions.put(name, "");
                            gameWaiting.put(name, Boolean.FALSE);
                            i++;
                        }

                        readCards();
                        if(chooseCards.size() == currentPlayers.size()) {
                            Object[] tempCards = chooseCards.toArray();
                            HashSet<Card> tempCards2 = (HashSet<Card>) chooseCards.clone();
                            for (Player player : currentPlayers) {
                                int random = (int) (Math.random() * tempCards.length);
                                player.card = (Card) tempCards[random];
                                tempCards2.remove(player.card);
                                tempCards = tempCards2.toArray();
                            }
                        } else if(chooseCards.size() > currentPlayers.size()) {
                            // If there are more cards than players
                            HashSet<Card> werewolves = new HashSet<Card>();
                            HashSet<Card> chooseCardsClone = (HashSet<Card>) chooseCards.clone();
                            for(Card card : chooseCards) {
                                if(card.cardName.equals("Werewolf") || card.cardName.equals("Wolf Man") || card.cardName.equals("Dire Wolf") || card.cardName.equals("Wolf Cub")) {
                                    werewolves.add(card);
                                    chooseCardsClone.remove(card);
                                }
                            }
                            int removeCardAmount = chooseCards.size() - currentPlayers.size();
                            for(i = 0; i < removeCardAmount; i++) {
                                int random = (int)(Math.random() * chooseCardsClone.size());
                                chooseCardsClone.remove(chooseCardsClone.toArray()[random]);
                            }
                            for(Card card : werewolves) {
                                chooseCardsClone.add(card);
                            }
                            Object[] tempCards = chooseCardsClone.toArray();
                            HashSet<Card> tempCards2 = (HashSet<Card>) chooseCardsClone.clone();
                            for (Player player : currentPlayers) {
                                int random = (int) (Math.random() * tempCards.length);
                                player.card = (Card) tempCards[random];
                                tempCards2.remove(player.card);
                                tempCards = tempCards2.toArray();
                            }



                        } else {
                            System.out.println("\nThere aren't enough cards for the amount of players.");
                            start = false;
                            continue;
                        }

                        for(Player player : currentPlayers) {
                            player.output.writeObject("Your card: " + player.card.cardName);
                        }

                        for(Card card : cards) {
                            if(card.nightWakeup) {
                                card.firstNightWakeup();
                                Thread.sleep(3000);
                                stopWaiting();
                            }
                        }

                        Card won = checkWins();
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

                        dead = new HashSet<Player>();

                        for(Player player : currentPlayers) {
                            if(player.dead) {
                                dead.add(player);
                            }
                        }
                        for(Player player : dead) {
                            sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!\n");
                            currentPlayers.remove(player);
                        }

                        while(true) {
                            amountOfDayKills = 1;
                            dayKillFlag = false;
                            votes = new HashMap<Player, Player>();
                            for (Player player : server.currentPlayers) {
                                votes.put(player, null);
                            }
                            sendToAllPlayers("\nNow everyone open your eyes. You all need to discuss and pick a person each that you will kill.\nThe number of people chosen to be killed is: " + amountOfDayKills);
                            new Thread(this::waitForAll).start();
                            new Thread(this::sendAllVotes).start();

                            while(!dayKillFlag) {
                                boolean good = true;

                                HashMap<Player, Integer> count = new HashMap<Player, Integer>();
                                for (Player player : server.currentPlayers) {
                                    count.put(player, 0);
                                }

                                for (Player player : votes.values()) {
                                    if (player != null) {
                                        count.replace(player, count.get(player) + 1);
                                    } else {
                                        good = false;
                                        break;
                                    }
                                }
                                if (!good) {
                                    continue;
                                }
                                dayKillFlag = true;
                                Thread.sleep(3000);
                                stopWaiting();

                                for (i = 0; i < amountOfDayKills; i++) {
                                    int highest = -1;
                                    Player dead2 = null;
                                    for (Player player : count.keySet()) {
                                        if (count.get(player) > highest) {
                                            highest = count.get(player);
                                            dead2 = player;
                                        }
                                    }
                                    dead2.dead = true;
                                    sendToAllPlayers(dead2.name + " has been chosen to be killed. Their card was: " + dead2.card.cardName);
                                    dead.add(dead2);
                                    currentPlayers.remove(dead2);
                                }
                            }

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

                            sendToAllPlayers("Now for the night. Everyone close your eyes.");
                            for(Card card : cards) {
                                if(card.nightWakeup) {
                                    card.nightWakeup();
                                    Thread.sleep(3000);
                                    stopWaiting();
                                }
                            }

                            for(Player player : currentPlayers) {
                                if(player.dead) {
                                    dead.add(player);
                                }
                            }
                            for(Player player : dead) {
                                sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!\n");
                                currentPlayers.remove(player);
                            }


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


                        gameStart = false;
                        currentPlayers.clear();
                    } else if(server.players.size() < 5) {
                        System.out.println("We need more people. Need at least 5");
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        private Card checkWins() {
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
                System.out.println("Checking win of " + card.cardName);
                if(card.won()) {
                    return card;
                }
            }
            return null;
        }

        private void sendAllVotes() {
            HashMap<Player, String> pastNames = new HashMap<Player, String>();
            for(Player player : currentPlayers) {
                pastNames.put(player, "");
            }
            while(!dayKillFlag) {
                for(Player player : currentPlayers) {
                    if(dayKillFlag) {
                        break;
                    }
                    if(!server.gameActions.get(player.name).equals("") && currentPlayers.contains(player) && !pastNames.get(player).equals(server.gameActions.get(player.name))) {
                        try {
                            sendToAllPlayers(player.name + " voted: " + server.gameActions.get(player.name));
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        votes.replace(player, server.players.get(server.gameActions.get(player.name)));
                        pastNames.replace(player, server.gameActions.get(player.name));
                    } else if(!gameActions.get(player.name).equals("") && !currentPlayers.contains(player)) {
                        try {
                            player.output.writeObject("Not a valid player");
                            server.gameActions.replace(player.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }

        private void waitForAll() {
            while(!dayKillFlag) {
                for (Player player : currentPlayers) {
                    if(dayKillFlag) {
                        break;
                    }
                    server.gameWaiting.replace(player.name, Boolean.TRUE);
                }
            }
        }

        private void readCards() throws FileNotFoundException {
            File file = new File("cards.txt");
            Scanner scanner = new Scanner(file);

            HashMap<Card, Card> temp = new HashMap<Card, Card>();
            HashSet<Card> temp2 = new HashSet<Card>();
            while(scanner.hasNextLine()) {
                String card = scanner.nextLine();
                String cardName;
                int cardAmount = 1;
                if(card.contains(" x")) {
                    cardName = card.substring(0, card.indexOf(" "));
                    cardAmount = Integer.parseInt(card.substring(card.indexOf("x")+1));
                } else {
                    cardName = card;
                }

                boolean doneOnce = false;
                for(int i = 0; i < cardAmount; i++) {
                    Card tempCard = null;
                    if (cardName.equalsIgnoreCase("villager") || cardName.equalsIgnoreCase("villagers")) {
                        tempCard = new VillagerCard(server);
                    } else if (cardName.equalsIgnoreCase("werewolf") || cardName.equalsIgnoreCase("werewolves")) {
                        tempCard = new WerewolfCard(server);
                    }

                    temp.put(tempCard, tempCard);
                    if(!doneOnce) {
                        temp2.add(tempCard);
                        doneOnce = true;
                    }
                }
            }
            scanner.close();

            server.chooseCards = new HashSet<Card>();
            int i = 0;
            for(Card card : temp.values()) {
                server.chooseCards.add(card);
                i++;
            }

            cards = new Card[temp2.size()];
            i = 0;
            for(Card card : temp2) {
                cards[i] = card;
                i++;
            }

            // Sort in order of rank
            for(i = 0; i < cards.length - 1; i++) {
                for(int j = 0; j < cards.length - i - 1; j++) {
                    Card temp3 = cards[j];
                    cards[j] = cards[j+1];
                    cards[j+1] = temp3;
                }
            }
        }
    }

    private void stopWaiting() {
        for (Player player : currentPlayers) {
            gameWaiting.replace(player.name, Boolean.FALSE);
            gameActions.replace(player.name, "");
        }
    }

    public void sendToAllPlayers(String message) throws IOException {
        for(Player player : players.values()) {
            player.output.writeObject(message);
        }
    }
}