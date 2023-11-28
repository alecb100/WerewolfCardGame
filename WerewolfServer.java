import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WerewolfServer implements Runnable {
    ConcurrentHashMap<String, ObjectOutputStream> players = new ConcurrentHashMap<String, ObjectOutputStream>();
    private ServerSocket ss;
    private ExecutorService clientThreadPool;
    String gameAction = "";
    final Object inputLock = new Object();

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
                    System.out.println("waiting...");
                    Object message = input.readObject();
                    String temp = (String)message;
                    String name = temp.substring(1, temp.indexOf(">"));
                    System.out.println(temp);
                    if(temp.charAt(0) == 'c') {
                        if(temp.substring(temp.indexOf(">") + 1).equals("")) {
                            continue;
                        }
                        handlePlayerCommand(name, temp.substring(temp.indexOf(">") + 1));
                    } else if(temp.charAt(0) == 'a') {
                        handleGameAction(name, temp.substring(temp.indexOf(">") + 1));
                    }
                }
            } catch(Exception e) {
                System.out.println(e.getMessage());
                server.removePlayer(playerName);
            }
        }

        private void handleGameAction(String name, String action) {
            if(!action.equals("")) {
                synchronized(inputLock) {
                    gameAction = name + ">" + action;
                    inputLock.notifyAll();
                }
            }
        }

        private void handlePlayerCommand(String name, String command) {
            try {
                players.get(name).writeObject("m>We got the message" + command);
                players.get(name).flush();
            } catch(Exception e) {
                System.out.println(e.getMessage());
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
                        for (String name : server.players.keySet()) {
                            playerArray[i] = name;
                            i++;
                        }
                        int randomPlayer = (int) (Math.random() * playerArray.length);
                        String randomPlayerName = playerArray[randomPlayer];
                        server.players.get(randomPlayerName).writeObject("g>Give me 3 (Press enter before giving number)");
                        server.players.get(randomPlayerName).flush();
                        while(true) {
                            synchronized (inputLock) {
                                while (gameAction.equals("")) {
                                    inputLock.wait();
                                }
                            }
                            if(gameAction.contains("3")) {
                                break;
                            } else {
                                server.players.get(randomPlayerName).writeObject("g>Not valid input");
                                server.players.get(randomPlayerName).flush();
                            }
                            gameAction = "";
                        }
                        if (gameAction.substring(0, gameAction.indexOf(">")).equals(randomPlayerName)) {
                            server.players.get(randomPlayerName).writeObject("><");
                            server.players.get(randomPlayerName).flush();
                            server.players.get(randomPlayerName).writeObject("g>woohooo!!!");
                            server.players.get(randomPlayerName).flush();
                        }
                        gameAction = "";
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
