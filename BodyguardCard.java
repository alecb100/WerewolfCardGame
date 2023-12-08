import java.util.HashMap;

// The class for the Bodyguard card, which protects a person from being killed during a night
public class BodyguardCard extends Card {
    HashMap<Player, Player> bodyguards;
    boolean ultraGood;

    // The constructor for the bodyguard card
    public BodyguardCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 100;
        this.team = "villager";
        this.cardName = "Bodyguard";
        this.winRank = 100;
        this.firstNightOnly = false;
    }

    // The help method for the bodyguard card
    @Override
    public String help() {
        String result = "The Bodyguard chooses one person to protect from being killed every night. They can protect a different ";
        result += "person every night and can also choose themselves. The only thing they cannot protect a person from is dying";
        result += " because a person they are linked with dies, like people who were chosen by the Cupid. They are on the ";
        result += "villagers team and count as a villager.";

        return result;
    }

    // The win method for the bodyguard card. Because it doesn't have its own special win condition, this method is
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

    // The night wakeup method where the bodyguard chooses a person to save during the night
    @Override
    public void nightWakeup() {
        // Tell everyone that the bodyguard is waking up
        try {
            server.sendToAllPlayers("Bodyguards, wake up, and determine who you want to protect. You can pick yourself.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine if there is a bodyguard or not and who they are
        bodyguards = new HashMap<Player, Player>();
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Bodyguard")) {
                bodyguards.put(player, null);
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    player.output.writeObject("Who do you wish to protect?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        // If there is a bodyguard, wait for the player to choose someone
        if(!bodyguards.isEmpty()) {
            // Continue in this while loop until they choose someone
            new Thread(this::sendToAllBodyguards).start();
            ultraGood = false;
            int count = 0;
            while(true) {
                // Run through the players currently alive and make sure it is a valid player
                for(Player bodyguard : bodyguards.keySet()) {
                    boolean checked = false;
                    if (bodyguards.get(bodyguard) == null && !server.gameActions.get(bodyguard.name).equals("")) {
                        for (Player player : server.currentPlayers) {
                            if (server.gameActions.get(bodyguard.name).equals(player.name)) {
                                bodyguards.replace(bodyguard, player);
                                server.gameActions.replace(bodyguard.name, "");
                                count++;
                            }
                        }
                        // Set a flag that their choice has been checked so that there's no issues that crop up with multiple
                        // threads and things being updated after this
                        checked = true;
                    }
                    if(checked && bodyguards.get(bodyguard) == null && !server.gameActions.get(bodyguard.name).equals("")) {
                        try {
                            bodyguard.output.writeObject("player not found.");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        server.gameActions.replace(bodyguard.name, "");
                    }
                }
                // Count up how many bodyguards have saved someone and stop the loops if all have asked
                if(count == bodyguards.size()) {
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
                server.sendToAllPlayers("Bodyguards, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no bodyguard, wait a random amount of time so the other players don't realize there is no bodyguard
            int randomWait = (int)(Math.random() * 5000) + 5000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Bodyguards, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void sendToAllBodyguards() {
        // Tell all seers who each other are
        for(Player player : bodyguards.keySet()) {
            try {
                player.output.writeObject("The bodyguards: " + bodyguards.keySet().toString());
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
        // Loop through the bodyguards, checking if they said a valid player and telling all the result
        while(!ultraGood) {
            // For each bodyguard
            for(Player bodyguard : bodyguards.keySet()) {
                // If they asked a valid person and this method hasn't already saw that they did and dealt with it
                if(bodyguards.get(bodyguard) != null && server.gameWaiting.get(bodyguard.name)) {
                    // Tell the server to stop waiting for this player and that their action is being dealt with
                    server.gameWaiting.replace(bodyguard.name, Boolean.FALSE);
                    // For each bodyguard again
                    for(Player bodyguard2 : bodyguards.keySet()) {
                        // Tell each of the original bodyguard's choice and the result
                        bodyguards.get(bodyguard).dead = false;
                        try {
                            bodyguard2.output.writeObject(bodyguard.name + " saved " + bodyguards.get(bodyguard).name);
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    // There is no special checkAfterDeath function for this card
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // There is no preCheck for this card
    @Override
    public void preCheck() {
        return;
    }
}
