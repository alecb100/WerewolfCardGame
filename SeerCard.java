// The class for the Seer card, who has the ability to see if 1 person is a werewolf or not every night
public class SeerCard extends Card {

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

    @Override
    public void nightWakeup() {
        // Tell everyone that the seer is waking up
        try {
            server.sendToAllPlayers("Seer, wake up, and pick who you want to see.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine if there is a seer or not (there would only ever be 1)
        Player seer = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Seer")) {
                seer = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    seer.output.writeObject("Who do you wish to see?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // If there is a seer, wait for the player to choose someone
        if(seer != null) {
            Player choice = null;
            // Continue in this while loop until they choose someone
            while(true) {
                if(!server.gameActions.get(seer.name).equals("")) {
                    // Run through the players currently alive and make sure it is a valid player
                    for(Player player : server.currentPlayers) {
                        // If it's a valid player, stop waiting for the seer
                        if(player.name.equals(server.gameActions.get(seer.name))) {
                            choice = player;
                            server.gameWaiting.replace(seer.name, Boolean.FALSE);
                            break;
                        }
                    }
                    // If it was a valid player, tell the seer whether they are a type of werewolf or not
                    if(choice != null) {
                        try {
                            if(server.checkWerewolf(choice)) {
                                seer.output.writeObject(choice.name + " is a type of Werewolf.");
                            } else {
                                seer.output.writeObject(choice.name + " is NOT a type of Werewolf.");
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        choice.dead = false;
                        try {
                            server.sendToAllPlayers("Seer, go back to sleep.");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return;
                    } else {
                        // If that player was not found, continue in the loop until they are
                        try {
                            seer.output.writeObject("Player not found");
                            server.gameActions.replace(seer.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        } else {
            // If there is no seer, wait a random amount of time so the other players don't realize there is no seer
            int randomWait = (int)(Math.random() * 5000) + 5000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Seer, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // There is no special checkAfterDeath function for this card, but there may be one implemented when creating the
    // Apprentice Seer
    @Override
    public void checkAfterDeath() {
        return;
    }
}
