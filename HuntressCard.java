import java.util.HashSet;

// The huntress can kill 1 person in a night if they so choose, but only once
public class HuntressCard extends Card {
    // The flag to see if the ability has been used this game already
    boolean abilityUsed;

    // The constructor to make the huntress card
    public HuntressCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 35;
        this.team = "villager";
        this.cardName = "Huntress";
        this.firstNightOnly = false;
        this.abilityUsed = false;
    }

    // The help method
    @Override
    public String help() {
        String result = "The Huntress is similar to the Hunter in that they can kill a player, but the timing of the kill ";
        result += "is very different. Instead of killing someone after the Huntress dies, the Huntress can choose to kill ";
        result += "anyone during the night. But they can only use the ability once. After they kill someone, they cannot kill ";
        result += "anyone during subsequent nights. Just like the Hunter, they are on the villagers team.";

        return result;
    }

    // The win condition, which is the same as the villager so it shouldn't get here
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

    // They don't have a special first night wakeup, so it calls the normal wakeup method
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    // The wakeup method, which asks the huntress if they want to use their ability, and if so, who
    @Override
    public void nightWakeup() {
        // Tell everyone the troublemaker is waking up
        try {
            server.sendToAllPlayers("\nNow Huntress, wake up.");
            server.sendToAllPlayers("Decide if you want to kill someone tonight. You can only use it once per game!\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Find the troublemaker, if they're still alive and there is one
        Player huntress = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Huntress")) {
                huntress = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                break;
            }
        }

        // If they're still alive
        if(huntress != null) {
            if(!abilityUsed) {
                // If the ability hasn't been used yet, ask if they want to use it
                try {
                    huntress.output.writeObject("Do you wish to use your ability? If so, choose a player. If not say no.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                // Create the thread for the timer
                Thread timer = null;
                if(server.timers[2] > 0) {
                    timer = new Thread(() -> huntressTimerHelper(server.timers[2]));
                    timer.start();
                }

                HashSet<String> potentials = new HashSet<String>();
                for(Player player : server.currentPlayers) {
                    if(player != huntress) {
                        potentials.add(player.name);
                    }
                }

                // Continuously wait until they say yes or no
                while(true) {
                    if(server.gameActions.get(huntress.name).equals("")) {
                        continue;
                    } else if(potentials.contains(server.gameActions.get(huntress.name))) {
                        // If they gave a player's name, stop waiting for them and use the ability
                        server.gameWaiting.replace(huntress.name, Boolean.FALSE);
                        abilityUsed = true;
                        // Set the chosen person to dead
                        for(Player player : server.currentPlayers) {
                            if(player.name.equals(server.gameActions.get(huntress.name))) {
                                player.dead = true;
                                break;
                            }
                        }
                        try {
                            huntress.output.writeObject("Your ability has been used to kill " + server.gameActions.get(huntress.name) + ". You cannot use it again.");
                            Thread.sleep(3000);
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        server.gameActions.replace(huntress.name, "");
                        break;
                    } else if(server.gameActions.get(huntress.name).equalsIgnoreCase("no") || server.gameActions.get(huntress.name).equalsIgnoreCase("n")) {
                        // If they say no, stop waiting for them and say they didn't use their ability
                        server.gameWaiting.replace(huntress.name, Boolean.FALSE);
                        server.gameActions.replace(huntress.name, "");
                        try {
                            huntress.output.writeObject("Your ability has not been used.");
                            Thread.sleep(3000);
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        break;
                    } else {
                        // If neither, say their reply was not recognized
                        try {
                            huntress.output.writeObject("Your reply was not recognized.");
                            server.gameActions.replace(huntress.name, "");
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
                    server.sendToAllPlayers("Huntress, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                // If the ability was already used, tell them as much and wait a random amount of time
                try {
                    huntress.output.writeObject("You have already used your ability.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                int randomWait = server.rand.nextInt(5000) + 5000;
                try {
                    Thread.sleep(randomWait);
                    server.sendToAllPlayers("Huntress, go back to sleep.\n");
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
                server.sendToAllPlayers("Huntress, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // There are no checks to be done after death
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check method, which makes sure there's only 1 huntress card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Huntress")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Huntress card.");
        }
    }

    // The method that deals with the timer
    private synchronized void huntressTimerHelper(int time) {
        // Get an array of the huntress
        Player[] huntressArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Huntress")) {
                huntressArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, huntressArray);

        // If it gets here, that means the players ran out of time, so tell the server no
        server.gameActions.replace(huntressArray[0].name, "no");
        try {
            Thread.sleep(500);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
