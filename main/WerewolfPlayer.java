package main;

import java.io.*;
import java.net.Socket;

public class WerewolfPlayer {
    Socket s;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    String playerName;
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    boolean pauseThreads = false;
    final Object inputLock = new Object();

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Invoke this program with <hostAddress> <Your Name>");
            return;
        }
        if(args[1].equalsIgnoreCase("na") || args[1].equalsIgnoreCase("no one")) {
            System.out.println("You cannot name yourself 'na' or 'no one'");
            return;
        }
        try {
            WerewolfPlayer player = new WerewolfPlayer(args[0], args[1]);
        } catch(Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    public WerewolfPlayer(String hostAddress, String playerName) throws Exception {
        this.playerName = playerName;
        if (hostAddress.contains(" ") || playerName.contains(" ")) {
            throw new IllegalArgumentException("Parameters may not contain blanks.");
        }
        System.out.println("Connecting to the game at " + hostAddress + ".");
        try {
            s = new Socket(hostAddress, 5555);
        } catch (Exception e) {
            throw new Exception("The connection was refused. Did you type the correct ip in? Is the server up?");
        }
        System.out.println("Connected to the Werewolf game server!");
        oos = new ObjectOutputStream(s.getOutputStream());
        oos.writeObject(playerName);
        ois = new ObjectInputStream(s.getInputStream());

        new Thread(this::listenToPrompts).start();

        new Thread(this::listenToPlayer).start();
    }

    private void listenToPrompts() {
        try {
            while (true) {
                Object incomingMessage = ois.readObject();
                String incoming = (String)incomingMessage;
                System.out.println(incoming);
            }
        } catch(Exception e) {
            System.out.println("Game error :(");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private void listenToPlayer() {
        while(true) {
            try {
                String input = br.readLine();
                oos.writeObject(input);
                oos.flush();
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
