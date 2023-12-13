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
        result += "are a type of werewolf or not. The werewolf types are Werewolf, Dire Wolf, Wolf Cub, and Wolf Man. However,";
        result += " there is a villager card that they see as a werewolf called the Lycan. Everything else counts as a villager.";

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

    // The night wakeup method where the server finds all the seers and asks each who they want to check.
    // It then tells all the other seers who they checked and the result of said check
    @Override
    public void nightWakeup() {
        // Tell everyone that the seers are waking up
        try {
            server.sendToAllPlayers("Seers, wake up, and pick who you want to see.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine if there is a seer or not and who they are
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

        // If there are seers, wait for them to choose someone
        if(!seers.isEmpty()) {
            // Continuously tell all other seers who a seer checked and the result
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
                        // Set a flag that their choice has been checked so that there's no issues that crop up with multiple
                        // threads and things being updated after this
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
                // Count up how many seers have been checked and stop the loops if all have asked
                if(count == seers.size()) {
                    try {
                        // Wait for the other thread to catch up
                        Thread.sleep(3000);
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                    // Stop that thread
                    ultraGood = true;
                    break;
                }
            }
            // Tell everyone that the seers are going back to sleep
            try {
                server.sendToAllPlayers("Seers, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no seer, wait a random amount of time so the other players don't realize there is no seer
            // The random time is the configured time
            int randomWait = server.rand.nextInt(server.idleTimes[0]) + server.idleTimes[1];
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Seers, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The method that makes sure all checks are sent to all seers
    private void sendToAllSeers() {
        // Tell all seers who each other are
        for(Player player : seers.keySet()) {
            try {
                player.output.writeObject("The seers: " + seers.keySet().toString());
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
        // Loop through the seers, checking if they said a valid player and telling all the result
        while(!ultraGood) {
            // For each seer
            for(Player seer : seers.keySet()) {
                // If they asked a valid person and this method hasn't already saw that they did and dealt with it
                if(seers.get(seer) != null && server.gameWaiting.get(seer.name)) {
                    // Tell the server to stop waiting for this player and that their action is being dealt with
                    server.gameWaiting.replace(seer.name, Boolean.FALSE);
                    // For each seer again
                    for(Player seer2 : seers.keySet()) {
                        // Tell each of the original seer's choice and the result
                        String result = "";
                        if(seers.get(seer).card.isSeenAsWerewolf) {
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

    // There is no preCheck that must be done for this card
    @Override
    public void preCheck() {
        return;
    }
}
