// The class for the Troublemaker card, who has the ability to make everyone kill 2 people the next day instead of 1
public class TroublemakerCard extends Card {
    // The flag to see if the ability has been used this game already
    boolean abilityUsed;

    // The constructor to make the troublemaker card
    public TroublemakerCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 70;
        this.team = "villager";
        this.cardName = "Troublemaker";
        this.firstNightOnly = false;
        this.abilityUsed = false;
    }

    // The help method for the bodyguard card
    @Override
    public String help() {
        String result = "The Troublemaker is just that, a troublemaker. They are on the villager team and is woken ";
        result += "up to decide if they want to use their ability or not. They can only use their ability once in the ";
        result += "entire game. Their ability is to force the villagers to kill 2 people rather than 1 the following day.";
        result += " They never have to use their ability in the game, but if they do choose to, they can't use it again.";

        return result;
    }

    // The win method for the troublemaker card. Because it doesn't have its own special win condition, this method is
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

    // The night wakeup method where the troublemaker decides if they want to use their ability, if they haven't already
    @Override
    public void nightWakeup() {
        // Tell everyone the troublemaker is waking up
        try {
            server.sendToAllPlayers("\nNow Troublemaker, wake up.");
            server.sendToAllPlayers("Decide if you want to force the deaths of an extra person the next day or not. You can only use it once per game!\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Find the troublemaker, if they're still alive and there is one
        Player troublemaker = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Troublemaker")) {
                troublemaker = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                break;
            }
        }

        // If they're still alive
        if(troublemaker != null) {
            if(!abilityUsed) {
                // If the ability hasn't been used yet, ask if they want to use it
                try {
                    troublemaker.output.writeObject("Do you wish to use your ability? (yes or no)\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                // Continuously wait until they say yes or no
                while(true) {
                    if(server.gameActions.get(troublemaker.name).equals("")) {
                        continue;
                    } else if(server.gameActions.get(troublemaker.name).equalsIgnoreCase("yes") || server.gameActions.get(troublemaker.name).equalsIgnoreCase("y")) {
                        // If they said yes, stop waiting for them and use the ability
                        server.gameWaiting.replace(troublemaker.name, Boolean.FALSE);
                        server.gameActions.replace(troublemaker.name, "");
                        abilityUsed = true;
                        // Set the amount of day kills to 2 and tell them they used their ability
                        server.amountOfDayKills = 2;
                        try {
                            troublemaker.output.writeObject("Your ability has been used. You cannot use it again.");
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        break;
                    } else if(server.gameActions.get(troublemaker.name).equalsIgnoreCase("no") || server.gameActions.get(troublemaker.name).equalsIgnoreCase("n")) {
                        // If they say no, stop waiting for them and say they didn't use their ability
                        server.gameWaiting.replace(troublemaker.name, Boolean.FALSE);
                        server.gameActions.replace(troublemaker.name, "");
                        try {
                            troublemaker.output.writeObject("Your ability has not been used.");
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        break;
                    } else {
                        // If neither, say their reply was not recognized
                        try {
                            troublemaker.output.writeObject("Your reply was not recognized.");
                            server.gameActions.replace(troublemaker.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                // Tell everyone the troublemaker is going back to sleep
                try {
                    server.sendToAllPlayers("Troublemaker, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                // If the ability was already used, tell them as much and wait a random amount of time
                try {
                    troublemaker.output.writeObject("You have already used your ability.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                int randomWait = server.rand.nextInt(5000) + 5000;
                try {
                    Thread.sleep(randomWait);
                    server.sendToAllPlayers("Troublemaker, go back to sleep.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } else {
            // If the troublemaker wasn't found, wait a random amount of time
            int randomWait = server.rand.nextInt(5000) + 5000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Troublemaker, go back to sleep.\n");
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

    // The pre check method that makes sure there's only 1 of this card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Troublemaker")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Troublemaker card.");
        }
    }
}
