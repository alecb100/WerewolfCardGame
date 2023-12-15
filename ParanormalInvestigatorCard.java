import java.util.HashSet;

// The paranormal investigator, which chooses a person once per game to see if them or one of their neighbors is a werewolf
public class ParanormalInvestigatorCard extends Card {
    // The flag to see if the ability has been used this game already
    boolean abilityUsed;

    // The constructor for the paranormal investigator
    public ParanormalInvestigatorCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Paranormal Investigator";
        this.nightWakeup = true;
        this.firstNightOnly = false;
        this.ranking = 21;
        this.abilityUsed = false;
    }

    // The help method
    @Override
    public String help() {
        String result = "The Paranormal Investigator is similar to the Seer. They are on the villager team and wake up ";
        result += "every night. However, where they differ is they can only use their ability once per game and they don't ";
        result += "see one player. They choose a player they wish to see, but instead of seeing just them, they see them and ";
        result += "their two neighbors. They will know if at least one of those 3 are a type of werewolf or not. Once they ";
        result += "use their ability, they cannot use it for the rest of the game.";

        return result;
    }

    // The win condition of the card, which is the same as the villager so it shouldn't get here
    @Override
    public boolean won() {
        boolean oneVillager = false;
        // Checking if there is at least 1 villager alive
        for(Player player : server.currentPlayers) {
            if(!player.card.team.contains("werewolf")) {
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

    // The first night wakeup, which is the same as every other night so call nightWakeup()
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    // The night wakeup method, which asks the investigator if they want to use their ability, in which case they give them a player
    @Override
    public void nightWakeup() {
        // Tell everyone the troublemaker is waking up
        try {
            server.sendToAllPlayers("\nNow Paranormal Investigator, wake up.");
            server.sendToAllPlayers("Decide if you want to see if a player or one of their neighbors is a type of wolf. ");
            server.sendToAllPlayers("You can only use it once per game!\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Find the troublemaker, if they're still alive and there is one
        Player investigator = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Paranormal Investigator")) {
                investigator = player;
                investigator.tookNightAction = true;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                break;
            }
        }

        // If they're still alive
        if(investigator != null) {
            if(!abilityUsed) {
                // If the ability hasn't been used yet, ask if they want to use it
                try {
                    investigator.output.writeObject("Do you wish to use your ability? If so, choose a player. If not say no.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                // Create the thread for the timer
                Thread timer = null;
                if(server.timers[2] > 0) {
                    timer = new Thread(() -> investigatorTimerHelper(server.timers[2]));
                    timer.start();
                }

                HashSet<String> potentials = new HashSet<String>();
                for(Player player : server.currentPlayers) {
                    potentials.add(player.name);
                }

                // Continuously wait until they say yes or no
                while(true) {
                    if(server.gameActions.get(investigator.name).equals("")) {
                        continue;
                    } else if(potentials.contains(server.gameActions.get(investigator.name))) {
                        // If they gave a player's name, stop waiting for them and use the ability
                        server.gameWaiting.replace(investigator.name, Boolean.FALSE);
                        abilityUsed = true;
                        // Set the chosen person to dead
                        Player chosen = null;
                        for(Player player : server.currentPlayers) {
                            if(player.name.equals(server.gameActions.get(investigator.name))) {
                                chosen = player;
                                break;
                            }
                        }
                        // Get a list of their neighbors
                        Player[] neighbors = server.neighborHelper(chosen);
                        try {
                            investigator.output.writeObject("Your ability has been used to check " + server.gameActions.get(investigator.name) + ". You cannot use it again.");
                            if(chosen.card.isSeenAsWerewolf || neighbors[0].card.isSeenAsWerewolf || neighbors[1].card.isSeenAsWerewolf) {
                                investigator.output.writeObject(chosen.name + " or one of their neighbors IS a type of Werewolf!");
                            } else {
                                investigator.output.writeObject(chosen.name + " and all of their neighbors is NOT a type of Werewolf!");
                            }
                            Thread.sleep(3000);
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        server.gameActions.replace(investigator.name, "");
                        break;
                    } else if(server.gameActions.get(investigator.name).equalsIgnoreCase("no") || server.gameActions.get(investigator.name).equalsIgnoreCase("n")) {
                        // If they say no, stop waiting for them and say they didn't use their ability
                        server.gameWaiting.replace(investigator.name, Boolean.FALSE);
                        server.gameActions.replace(investigator.name, "");
                        try {
                            investigator.output.writeObject("Your ability has not been used.");
                            Thread.sleep(3000);
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        break;
                    } else {
                        // If neither, say their reply was not recognized
                        try {
                            investigator.output.writeObject("Your reply was not recognized.");
                            server.gameActions.replace(investigator.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }

                // Stop the timer
                if(server.timers[2] > 0) {
                    timer.interrupt();
                }

                // Tell everyone the troublemaker is going back to sleep
                try {
                    server.sendToAllPlayers("Paranormal Investigator, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                // If the ability was already used, tell them as much and wait a random amount of time
                try {
                    investigator.output.writeObject("You have already used your ability.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                int randomWait = server.rand.nextInt(5000) + 5000;
                try {
                    Thread.sleep(randomWait);
                    server.sendToAllPlayers("Paranormal Investigator, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } else {
            // If the troublemaker wasn't found, wait a random amount of time
            // The random time is the configured time
            int randomWait = server.rand.nextInt(server.idleTimes[0]) + server.idleTimes[1];
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Paranormal Investigator, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // There are no checks after death to do for this card
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check method, which checks if there's more than 1 of this card or if neighbors isn't up-to-date
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Paranormal Investigator")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Paranormal Investigator card.");
        }

        // Checks if neighbors are specified
        if(server.neighbors == null) {
            throw new IllegalArgumentException("Neighbors aren't specified.");
        }

        // Checks if all players in neighbors are in currentPlayers
        for(Player player : server.neighbors) {
            if(!server.currentPlayers.contains(player)) {
                throw new IllegalArgumentException("Neighbors is not up to date.");
            }
        }

        // Checks if all players in currentPlayers are in neighbors
        for(Player player : server.currentPlayers) {
            boolean good = false;
            // Loop through neighbors trying to see if this player is in neighbors
            for(Player neighbor : server.neighbors) {
                if(neighbor == player) {
                    good = true;
                    break;
                }
            }
            // If the player is not in neighbors, tell the server moderator
            if(!good) {
                throw new IllegalArgumentException("Neighbors is not up to date.");
            }
        }
    }

    // The method that deals with the timer
    private synchronized void investigatorTimerHelper(int time) {
        // Get an array of the huntress
        Player[] investigatorArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Paranormal Investigator")) {
                investigatorArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, investigatorArray);

        // If it gets here, that means the players ran out of time, so tell the server no
        server.gameActions.replace(investigatorArray[0].name, "no");
        try {
            Thread.sleep(500);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
