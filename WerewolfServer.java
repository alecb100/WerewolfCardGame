import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
    HashSet<Card> cards;

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
                System.out.println("New player joined from " + clientAddress + ", " + playerName);

                this.player = new Player(playerName, input, output);
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
                    players.get(playerName).output.writeObject("We got the message: " + command);
                    players.get(playerName).output.flush();
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
                    players.get(playerName).output.writeObject("We got the action: " + action);
                    players.get(playerName).output.flush();
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
                        Object[] tempCards = chooseCards.toArray();
                        HashSet<Card> tempCards2 = (HashSet<Card>) chooseCards.clone();
                        for(Player player : currentPlayers) {
                            int random = (int)(Math.random() * tempCards.length);
                            player.card = (Card)tempCards[random];
                            tempCards2.remove(player.card);
                            tempCards = tempCards2.toArray();
                        }

                        for(Player player : currentPlayers) {
                            player.output.writeObject("Your card: " + player.card.cardName);
                        }

                        for(Card card : cards) {
                            if(card.nightWakeup) {
                                card.nightWakeup();
                            }
                        }

                        for(Player player : currentPlayers) {
                            if(player.dead) {
                                sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!");
                            }
                        }


                        gameStart = false;
                    } else if(server.players.size() < 5) {
                        System.out.println("We need more people. Need at least 5");
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        private void readCards() throws FileNotFoundException {
            File file = new File("cards.txt");
            Scanner scanner = new Scanner(file);

            HashMap<Card, Card> temp = new HashMap<Card, Card>();
            server.cards = new HashSet<Card>();
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
                        server.cards.add(tempCard);
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
        }
    }

    public void sendToAllPlayers(String message) throws IOException {
        for(Player player : currentPlayers) {
            player.output.writeObject(message);
        }
    }
}