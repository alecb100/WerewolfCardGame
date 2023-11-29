import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WerewolfServer implements Runnable {
    ConcurrentHashMap<String, ObjectOutputStream> players = new ConcurrentHashMap<String, ObjectOutputStream>();
    private ServerSocket ss;
    private ExecutorService clientThreadPool;
    HashMap<String, String> gameActions;
    boolean gameStart = false;
    HashMap<String, Boolean> gameWaiting;

    public static void main(String[] args) throws IOException {
        new WerewolfServer();
    }

    WerewolfServer() throws IOException {
        ss = new ServerSocket(5555);
        System.out.println("Werewolf Server is up at " + InetAddress.getLocalHost().getHostAddress() + " on port " + ss.getLocalPort());
        clientThreadPool = Executors.newCachedThreadPool();
        new Thread(this).start();
    }

    public void addPlayer(String playerName, ObjectOutputStream outStream) {
        players.put(playerName, outStream);
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

                server.addPlayer(playerName, output);


                while(true) {
                    System.out.println("waiting for " + playerName);
                    Object message = input.readObject();
                    String temp = (String)message;
                    System.out.println(playerName + ": " + temp);
                    if(gameStart && gameWaiting.get(playerName)) {
                        if(temp.contains("help")) {
                            new Thread(new handlePlayerCommand(temp.substring(temp.indexOf(":")+1))).start();
                            continue;
                        }
                        new Thread(new handleGameAction(temp)).start();
                    } else {
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
                    players.get(playerName).writeObject("We got the message: " + command);
                    players.get(playerName).flush();
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
                    players.get(playerName).writeObject("We got the action: " + action);
                    players.get(playerName).flush();
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

                    if (start) {
                        System.out.println("Starting");
                        String[] playerArray = new String[server.players.size()];
                        int i = 0;
                        gameActions = new HashMap<String, String>();
                        gameWaiting = new HashMap<String, Boolean>();
                        for (String name : server.players.keySet()) {
                            playerArray[i] = name;
                            gameActions.put(name, "");
                            gameWaiting.put(name, Boolean.FALSE);
                            i++;
                        }
                        int randomPlayer = (int) (Math.random() * playerArray.length);
                        String randomPlayerName = playerArray[randomPlayer];
                        server.players.get(randomPlayerName).writeObject("Give me 3 (Press enter before giving number)");
                        server.players.get(randomPlayerName).flush();
                        gameWaiting.replace(randomPlayerName, Boolean.TRUE);
                        while(true) {
                            if(gameActions.get(randomPlayerName).contains("3")) {
                                break;
                            } else {
                                server.players.get(randomPlayerName).writeObject("Not valid input");
                                server.players.get(randomPlayerName).flush();
                            }
                            gameActions.replace(randomPlayerName, "");
                        }
                        if(!gameActions.get(randomPlayerName).equals("")) {
                            server.players.get(randomPlayerName).writeObject("woohooo!!!");
                            server.players.get(randomPlayerName).flush();
                            gameWaiting.replace(randomPlayerName, Boolean.FALSE);
                        }
                        gameActions.replace(randomPlayerName, "");
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}