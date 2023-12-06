import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

// The class for the Seer card, who has the ability to see if 1 person is a werewolf or not every night
public class SeerCard extends Card {
    HashMap<Player, Player> seers;
    boolean ultraGood;

    // The constructor to make the seer card
    public SeerCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 20;
        this.team = "villager";
        this.cardName = "Seer";
        this.firstNightOnly = false;
    }

    // The help method for the bodyguard card
    @Override
    public String help() {
        String result = "The seer is one of the most important villagers in the game. They have the ability to see if ";
        result += "a player is a type of werewolf. Every night, they are asked who they want to check and are told if they ";
        result += "are a type of werewolf or not. The werewolf types are Werewolf, Dire Wolf, Wolf Cub, and Wolf Man. They ";
        result += "count as a villager for everything else, including wins.";

        return result;
    }

    // The win method for the seer card. Because it doesn't have its own special win condition, this method is
    // copied and pasted from the villager card
    @Override
    public boolean won() {
        boolean oneVillager = false;
        // Checking if there is at least 1 villager alive
        for(Player player : server.currentPlayers) {
            if(!player.card.team.equals("werewolf")) {
                oneVillager = true;
                break;
            }
        }
        if(!oneVillager) {
            return false;
        }

        // Checking to make sure all werewolves are dead
        boolean result = true;
        for(Player player : server.currentPlayers) {
            if(!player.dead && server.checkWerewolf(player)) {
                result = false;
                break;
            }
        }

        return result;
    }

    // No special first night wakeup, so call the normal night wakeup method
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    @Override
    public void nightWakeup() {
        // Tell everyone that the seer is waking up
        try {
            server.sendToAllPlayers("Seers, wake up, and pick who you want to see.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine if there is a seer or not
        seers = new HashMap<Player, Player>();
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Seer")) {
                seers.put(player, null);
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    player.output.writeObject("Who do you wish to see?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // If there is a seer, wait for the player to choose someone
        if(!seers.isEmpty()) {
            new Thread(this::sendToAllSeers).start();
            ultraGood = false;
            int count = 0;
            // Continue in this while loop until all choose someone
            while(true) {
                // Run through the players currently alive and make sure it is a valid player
                for(Player seer : seers.keySet()) {
                    boolean checked = false;
                    if(seers.get(seer) == null && !server.gameActions.get(seer.name).equals("")) {
                        for(Player player : server.currentPlayers) {
                            if(server.gameActions.get(seer.name).equals(player.name)) {
                                seers.replace(seer, player);
                                server.gameActions.replace(seer.name, "");
                                count++;
                            }
                        }
                        checked = true;
                    }
                    if(checked && seers.get(seer) == null && !server.gameActions.get(seer.name).equals("")) {
                        try {
                            seer.output.writeObject("Player not found.");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        server.gameActions.replace(seer.name, "");
                    }
                }
                if(count == seers.size()) {
                    try {
                        Thread.sleep(3000);
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                    ultraGood = true;
                    break;
                }
            }
            try {
                server.sendToAllPlayers("Seers, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no seer, wait a random amount of time so the other players don't realize there is no seer
            int randomWait = (int)(Math.random() * 5000) + 5000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Seers, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void sendToAllSeers() {
        for(Player player : seers.keySet()) {
            try {
                player.output.writeObject("The seers: " + seers.keySet().toString());
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
        while(!ultraGood) {
            for(Player seer : seers.keySet()) {
                if(seers.get(seer) != null && server.gameWaiting.get(seer.name)) {
                    server.gameWaiting.replace(seer.name, Boolean.FALSE);
                    for(Player seer2 : seers.keySet()) {
                        String result = "";
                        if(server.checkWerewolf(seers.get(seer)) || seers.get(seer).card.cardName.equals("Lycan")) {
                            result = seers.get(seer) + " IS a type of Werewolf.";
                        } else {
                            result = seers.get(seer) + " is NOT a type of Werewolf.";
                        }
                        try {
                            seer2.output.writeObject(seer.name + " checked " + seers.get(seer).name + ": " + result);
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    // There is no special checkAfterDeath function for this card, but there may be one implemented when creating the
    // Apprentice Seer
    @Override
    public void checkAfterDeaths() {
        return;
    }
}
