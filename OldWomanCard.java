import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// The old woman role, which is a villager that makes it so a player can't vote or speak during voting the next day
public class OldWomanCard extends Card {
    Set<Player> previousChosen;

    // The constructor for the old woman
    public OldWomanCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Old Woman";
        this.nightWakeup = true;
        this.firstNightOnly = false;
        this.ranking = 30;
        this.previousChosen = ConcurrentHashMap.newKeySet();
    }

    // The help method for this card
    @Override
    public String help() {
        String result = "The Old Woman is a weird card. They are on the villager team, but their ability is to silence 1";
        result += " person every night. This person cannot be someone they previously silenced before, and they cannot silence ";
        result += "themselves. Once a person is silenced, they cannot speak or vote during that day. Once everyone votes, ";
        result += "it will appear like they still voted as the server will display a vote, but it didn't actually count. ";
        result += "Once the old woman has exhausted all of her options for silencing, she will no longer be able to silence anyone.";

        return result;
    }

    // The win condition for the old woman, which is the same as the villager so it shouldn't get here
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

    // The first night wakeup is the same as the normal wakeup, so call that
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    // The night wakeup method, which asks the old woman to pick someone to silence the next day, which they then can't
    // silence again
    @Override
    public void nightWakeup() {
        // Tell everyone that the Old Woman is waking up
        try {
            server.sendToAllPlayers("\nOld Woman, wake up and choose someone to silence the next day. That player cannot ");
            server.sendToAllPlayers("speak or vote during that day. You cannot choose yourself or a previously chosen player.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine who the Old Woman is
        Player oldWoman = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Old Woman")) {
                oldWoman = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    oldWoman.output.writeObject("Who do you want to silence the next day?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                // Check if they are in the previous chosen, which will be used to determine which players aren't valid choices
                if(!previousChosen.contains(oldWoman)) {
                    previousChosen.add(oldWoman);
                }
                break;
            }
        }

        // Check if there are even anyone to pick
        HashSet<Player> potentials = new HashSet<Player>(server.currentPlayers);
        for(Player player : server.currentPlayers) {
            if(previousChosen.contains(player)) {
                potentials.remove(player);
            }
        }
        if(potentials.size() == 0) {
            if(oldWoman != null) {
                try {
                    oldWoman.output.writeObject("There is no one left to silence.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            oldWoman = null;
        }

        if(oldWoman != null) {
            // Get all the valid players to choose (all players that aren't werewolves)
            HashSet<Player> choices = new HashSet<Player>();
            for (Player player : server.currentPlayers) {
                if (!previousChosen.contains(player)) {
                    choices.add(player);
                }
            }

            // Wait for the player to choose someone
            Player choice = null;

            // Create the thread for the timer
            Thread timer = null;
            if (server.timers[2] > 0) {
                timer = new Thread(() -> oldWomanTimerHelper(server.timers[2]));
                timer.start();
            }

            // Continue in this while loop until they choose someone
            while (true) {
                if (!server.gameActions.get(oldWoman.name).equals("")) {
                    // Run through the players currently alive and make sure it is a valid player
                    for (Player player : choices) {
                        // If it's a valid player, stop waiting for the old woman
                        if (player.name.equals(server.gameActions.get(oldWoman.name))) {
                            choice = player;
                            server.gameWaiting.replace(oldWoman.name, Boolean.FALSE);
                            break;
                        }
                    }
                    // If it was a valid player, tell the old woman their choice and save it
                    if (choice != null) {
                        // Stop the timer
                        if (server.timers[2] > 0) {
                            timer.interrupt();
                        }
                        try {
                            oldWoman.output.writeObject(choice.name + " will be silenced.");
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        // Tell the player that they can not vote or speak
                        choice.canVote = false;
                        previousChosen.add(choice);
                        try {
                            Thread.sleep(3000);
                            server.sendToAllPlayers("\nOld Woman, go back to sleep.");
                            break;
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return;
                    } else {
                        // If that player was not found, continue in the loop until they are
                        try {
                            oldWoman.output.writeObject("Player not found");
                            server.gameActions.replace(oldWoman.name, "");
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
            try {
                Thread.sleep(3000);
                server.sendToAllPlayers("\nNow, the player that was silenced will be woken up realize what's happened.\n");
                choice.output.writeObject("You have been silenced for the next day only. That means you cannot speak or vote.");
                Thread.sleep(3000);
                server.sendToAllPlayers("\nSilenced player, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no old woman, wait a random amount of time so the other players don't realize there is no old woman
            // The random time is the idle time given
            int randomWait = server.rand.nextInt(server.idleTimes[0]) + server.idleTimes[1];
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("\nOld Woman, go back to sleep.");
                Thread.sleep(3000);
                server.sendToAllPlayers("\nNow, the player that was silenced will be woken up realize what's happened.\n");
                Thread.sleep(3000);
                server.sendToAllPlayers("\nSilenced player, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // There are no check after deaths that must be done for this card
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check for this card, which ensures there's only 1 of them
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Old Woman")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Old Woman card.");
        }
    }

    // The method that deals with the timer
    private synchronized void oldWomanTimerHelper(int time) {
        // Get an array of the Old Woman
        Player[] oldWomanArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Old Woman")) {
                oldWomanArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, oldWomanArray);

        // If it gets here, that means the players ran out of time, so set all who haven't chosen to a random player
        HashSet<Player> potentials = new HashSet<Player>(server.currentPlayers);
        for(Player player : server.currentPlayers) {
            if(previousChosen.contains(player)) {
                potentials.remove(player);
            }
        }

        // Set the companion as a random player
        int random = server.rand.nextInt(potentials.size());
        server.gameActions.replace(oldWomanArray[0].name, potentials.toArray()[random].toString());
        try {
            Thread.sleep(500);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
