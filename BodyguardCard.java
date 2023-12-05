// The class for the Bodyguard card, which protects a person from being killed during a night
public class BodyguardCard extends Card {

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
            if(!player.dead && player.card.team.equals("werewolf")) {
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
            server.sendToAllPlayers("Bodyguard, wake up, and determine who you want to protect. You can pick yourself.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine if there is a bodyguard or not (there would only ever be 1)
        Player bodyguard = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Bodyguard")) {
                bodyguard = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    bodyguard.output.writeObject("Who do you wish to protect?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // If there is a bodyguard, wait for the player to choose someone
        if(bodyguard != null) {
            Player choice = null;
            // Continue in this while loop until they choose someone
            while(true) {
                if(!server.gameActions.get(bodyguard.name).equals("")) {
                    // Run through the players currently alive and make sure it is a valid player
                    for(Player player : server.currentPlayers) {
                        // If it's a valid player, stop waiting for the bodyguard
                        if(player.name.equals(server.gameActions.get(bodyguard.name))) {
                            choice = player;
                            server.gameWaiting.replace(bodyguard.name, Boolean.FALSE);
                            break;
                        }
                    }
                    // If it was a valid player, tell the bodyguard their choice and set that player as not dead
                    // regardless if they were or not
                    if(choice != null) {
                        try {
                            bodyguard.output.writeObject("Player saved: " + choice.name);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        choice.dead = false;
                        try {
                            server.sendToAllPlayers("Bodyguard, go back to sleep.");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return;
                    } else {
                        // If that player was not found, continue in the loop until they are
                        try {
                            bodyguard.output.writeObject("Player not found");
                            server.gameActions.replace(bodyguard.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        } else {
            // If there is no bodyguard, wait a random amount of time so the other players don't realize there is no bodyguard
            int randomWait = (int)(Math.random() * 5000) + 5000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Bodyguard, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // There is no special checkAfterDeath function for this card
    @Override
    public void checkAfterDeaths() {
        return;
    }
}
