package main.Cards;

import main.Player;
import main.WerewolfServer;

import java.util.HashSet;

// The witch card, which wakes up every night and can see who is marked for death, and once per game, choose to save one or kill another
public class WitchCard extends Card {
    // The flag for whether the ability has been used or not
    boolean abilityUsed;

    // The constructor for the witch card
    public WitchCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 99;
        this.team = "villager";
        this.cardName = "Witch";
        this.firstNightOnly = false;
        this.abilityUsed = false;
    }

    // The help method
    @Override
    public String help() {
        String result = "The Witch is an interesting card. They can only use their ability once per game, where every night ";
        result += "they wake up and see who is marked for death that night. They can choose to save one of those people, ";
        result += "kill another player, or save their aiblity. However, if they choose to do either, they use their ability. ";
        result += "After they use their ability, they can no longer see who is marked for death any more.";

        return result;
    }

    // The win condition, which is the same as the villager, so it shouldn't get here
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

    // The first night wakeup is not special, so call the normal night wakup method
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    // Every night the witch is woken up to learn who is marked for death. They can choose to use their ability to either
    // save one or kill another. If they use it, they can't use it again for the rest of the game and also can't see
    // who is marked for death anymore
    @Override
    public void nightWakeup() {
        // Tell everyone the witch is waking up
        try {
            server.sendToAllPlayers("\nWitch, wake up.");
            server.sendToAllPlayers("Decide if you want to save a player that will die, kill another one, or neither. ");
            server.sendToAllPlayers("You can only save or kill someone once per game!\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Find the witch, if they're still alive and there is one
        Player witch = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Witch")) {
                witch = player;
                witch.tookNightAction = true;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                break;
            }
        }

        // If they're still alive
        if(witch != null) {
            if(!abilityUsed) {
                // If the ability hasn't been used yet, ask if they want to use it
                try {
                    witch.output.writeObject("Do you wish to use your ability? If so, choose a player. If not say no. ");
                    witch.output.writeObject("If you put a player that is not marked for death, you will kill them. ");
                    witch.output.writeObject("Otherwise, you will save them.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                HashSet<String> markedForDeath = new HashSet<String>();
                for(Player player : server.currentPlayers) {
                    if(player.dead) {
                        markedForDeath.add(player.name);
                    }
                }
                try {
                    witch.output.writeObject("The players marked for death: " + markedForDeath.toString());
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                // Create the thread for the timer
                Thread timer = null;
                if(server.timers[2] > 0) {
                    timer = new Thread(() -> witchTimerHelper(server.timers[2]));
                    timer.start();
                }

                HashSet<String> potentials = new HashSet<String>();
                for(Player player : server.currentPlayers) {
                    potentials.add(player.name);
                }

                // Continuously wait until they say yes or no
                while(true) {
                    if(server.gameActions.get(witch.name).equals("")) {
                        continue;
                    } else if(potentials.contains(server.gameActions.get(witch.name))) {
                        // If they gave a player's name, stop waiting for them and use the ability
                        server.gameWaiting.replace(witch.name, Boolean.FALSE);
                        abilityUsed = true;
                        // Get the chosen player
                        Player chosen = null;
                        for(Player player : server.currentPlayers) {
                            if(player.name.equals(server.gameActions.get(witch.name))) {
                                chosen = player;
                                break;
                            }
                        }
                        // Set the chosen player as the opposite of their current situation, death-wise
                        chosen.dead = !chosen.dead;
                        server.gameActions.replace(witch.name, "");
                        break;
                    } else if(server.gameActions.get(witch.name).equalsIgnoreCase("no") || server.gameActions.get(witch.name).equalsIgnoreCase("n")) {
                        // If they say no, stop waiting for them and say they didn't use their ability
                        server.gameWaiting.replace(witch.name, Boolean.FALSE);
                        server.gameActions.replace(witch.name, "");
                        try {
                            witch.output.writeObject("Your ability has not been used.");
                            Thread.sleep(3000);
                        } catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        break;
                    } else {
                        // If neither, say their reply was not recognized
                        try {
                            witch.output.writeObject("Your reply was not recognized.");
                            server.gameActions.replace(witch.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                // Stop the timer
                if(server.timers[2] > 0) {
                    timer.interrupt();
                }

                // Tell everyone the troublemaker is going back to sleep
                try {
                    server.sendToAllPlayers("Witch, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                // If the ability was already used, tell them as much and wait a random amount of time
                try {
                    witch.output.writeObject("You have already used your ability.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                int randomWait = server.rand.nextInt(5000) + 5000;
                try {
                    Thread.sleep(randomWait);
                    server.sendToAllPlayers("Witch, go back to sleep.\n");
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
                server.sendToAllPlayers("Witch, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // There is no special check after death to be done for this card
    @Override
    public void checkAfterDeaths() {

    }

    // The pre check, which makes sure there's only 1 instance of this card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Witch")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Witch card.");
        }
    }

    // The method that deals with the timer
    private synchronized void witchTimerHelper(int time) {
        // Get an array of the witch
        Player[] witchArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Witch")) {
                witchArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, witchArray);

        // If it gets here, that means the players ran out of time, so tell the server no
        server.gameActions.replace(witchArray[0].name, "no");
        try {
            Thread.sleep(500);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
