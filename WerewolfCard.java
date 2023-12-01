import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class WerewolfCard extends Card {
    HashMap<Player, Player> werewolves;
    boolean ultraGood;

    public WerewolfCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 45;
        this.team = "werewolf";
        this.cardName = "Werewolf";
        this.winRank = 5;
        this.firstNightOnly = false;
    }

    @Override
    public String help() {
        String result = "This is the plain Werewolf, the main werewolf of the game. They wake up every night and choose one person to kill (doing that every night). ";
        result += "They win by eliminating enough villagers so that the amount of alive werewolves equal or is greater than the ";
        result += "amount of alive villagers. There are other werewolves in the game, including the Dire Wolf, the Wolf Man, and the Werewolf Cub.";
        result += " They also have helpers, such as the minion, who do not actually count as werewolves but are on the werewolves team.";
        return result;
    }

    @Override
    public boolean won() {
        int werewolfCount = 0;
        int otherCount = 0;
        for(Player player : server.currentPlayers) {
            if(checkWerewolf(player)) {
                werewolfCount++;
            } else {
                otherCount++;
            }
        }
        return werewolfCount >= otherCount;
    }

    private boolean checkWerewolf(Player player) {
        return player.card.cardName.equals("Werewolf") || player.card.cardName.equals("Dire Werewolf") ||
                player.card.cardName.equals("Werewolf Cub") || player.card.cardName.equals("Wolf Man");
    }

    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    @Override
    public void nightWakeup() {
        // See how many werewolves there are
        werewolves = new HashMap<Player, Player>();
        for (Player player : server.currentPlayers) {
            if(checkWerewolf(player)) {
                werewolves.put(player, null);
            }
        }

        // Tell everyone that werewolves are waking up
        try {
            server.sendToAllPlayers("All werewolves, wake up, and determine who you want to kill.\nAll werewolves must vote. The player with the most votes will be killed.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        try {
            outputPrint("Other Werewolves: " + werewolves.keySet().toString());
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Wait for werewolves choice of kill
        new Thread(this::sendToWerewolves).start();
        for(Player player : werewolves.keySet()) {
            server.gameWaiting.replace(player.name, Boolean.TRUE);
        }
        while(true) {
            ultraGood = false;
            boolean good = true;

            HashMap<Player, Integer> count = new HashMap<Player, Integer>();
            for(Player player : server.currentPlayers) {
                if(!checkWerewolf(player)) {
                    count.put(player, 0);
                }
            }

            for(Player player : werewolves.values()) {
                if(player != null) {
                    count.replace(player, count.get(player)+1);
                } else {
                    good = false;
                    break;
                }
            }
            if(!good) {
                continue;
            }
            ultraGood = true;
            server.stopWaiting();
            try {
                Thread.sleep(3000);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }

            int highest = -1;
            Player dead = null;
            for(Player player : count.keySet()) {
                if(count.get(player) > highest) {
                    highest = count.get(player);
                    dead = player;
                }
            }

            dead.dead = true;
            System.out.println("Werewolves' chosen kill: " + dead.name);
            break;
        }

        try {
            server.sendToAllPlayers("All werewolves, go back to sleep.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendToWerewolves() {
        String[] names = new String[server.currentPlayers.size()-werewolves.size()];
        int i = 0;
        for(Player player : server.currentPlayers) {
            if(!checkWerewolf(player) && !player.dead && !player.tower) {
                names[i] = player.name;
                i++;
            }
        }
        HashMap<Player, String> pastNames = new HashMap<Player, String>();
        for(Player player : werewolves.keySet()) {
            pastNames.put(player, "");
        }
        while(!ultraGood) {
            for(Player player : werewolves.keySet()) {
                if(!server.gameActions.get(player.name).equals("") && Arrays.asList(names).contains(server.gameActions.get(player.name)) && !pastNames.get(player).equals(server.gameActions.get(player.name))) {
                    try {
                        outputPrint(player.name + " voted: " + server.gameActions.get(player.name));
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                    werewolves.replace(player, server.players.get(server.gameActions.get(player.name)));
                    pastNames.replace(player, server.gameActions.get(player.name));
                } else if(!server.gameActions.get(player.name).equals("") && !Arrays.asList(names).contains(server.gameActions.get(player.name))) {
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

    private void outputPrint(String s) throws IOException {
        for(Player player : werewolves.keySet()) {
            player.output.writeObject(s);
            player.output.flush();
        }
    }

    @Override
    public void checkAfterDeath() { return; }
}