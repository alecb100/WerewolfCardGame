package main.Cards;

import main.Player;
import main.WerewolfServer;

import java.util.Arrays;
import java.util.HashSet;

// The Hoodlum card, which is on a team of its own and wins when everyone they chose in the beginning is dead
public class HoodlumCard extends Card {
    // The people who the Hoodlum wants dead
    Player[] playerDeathWish;

    // The constructor for creating the Hoodlum
    public HoodlumCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 16;
        this.team = "hoodlum";
        this.cardName = "Hoodlum";
        this.winRank = 1;
        this.firstNightOnly = true;
    }

    @Override
    public String help() {
        String result = "The Hoodlum is on a team completely on their own. They choose an amount of players equal to the ";
        result += "number of werewolves in the game during the first night. When those people die, they win, even if the ";
        result += "last werewolf was killed. They have priority over the werewolves and villagers in terms of winning. ";
        result += "Obviously, they have to be alive to win, though.";

        return result;
    }

    // The win condition for Hoodlum. Checks if all the players they chose in the beginning is dead
    @Override
    public boolean won() {
        // Checks to see if the Hoodlum is alive
        boolean alive = false;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Hoodlum")) {
                alive = true;
                break;
            }
        }

        // If the Hoodlum is not alive, they did not win
        if(!alive) {
            return false;
        }

        // Check all death wished players
        for(Player player : playerDeathWish) {
            // If any are still alive, the Hoodlum did not win
            if(!player.dead) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void firstNightWakeup() {// Tell everyone that the Cupid is waking up
        try {
            server.sendToAllPlayers("\nHoodlum, wake up and choose " + server.werewolfNum + " people you want to be dead. ");
            server.sendToAllPlayers("If all die, then you win. This is the only way that you can win.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Checks if there is a Hoodlum
        Player hoodlum = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Hoodlum")) {
                hoodlum = player;
                hoodlum.tookNightAction = true;
                try {
                    hoodlum.output.writeObject("\nHoodlum, which " + server.werewolfNum + " players do you wish to choose? Put them each on separate lines please.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // If there is a Hoodlum, tell them to pick the players
        if(hoodlum != null) {
            playerDeathWish = new Player[server.werewolfNum];

            //Create the thread for the timer
            Thread timer = null;
            if(server.timers[2] > 0) {
                timer = new Thread(() -> hoodlumTimerHelper(server.timers[2] + (10000*(server.werewolfNum-1))));
                timer.start();
            }

            // Run through a werewolf num length loop of them picking people
            for (int i = 0; i < server.werewolfNum; i++) {
                server.gameWaiting.replace(hoodlum.name, Boolean.TRUE);
                while(true) {
                    // If they made an action
                    if (!server.gameActions.get(hoodlum.name).equals("")) {
                        // If the Hoodlum didn't choose themselves
                        if(!server.gameActions.get(hoodlum.name).equals(hoodlum.name)) {
                            // Run through the players currently alive and make sure it is a valid player
                            for (Player player : server.currentPlayers) {
                                // If the player was previously chosen, don't allow them to be the next chosen
                                boolean goodChosen = true;
                                for (int j = 0; j < i; j++) {
                                    if (server.gameActions.get(hoodlum.name).equals(playerDeathWish[j].name)) {
                                        goodChosen = false;
                                        break;
                                    }
                                }
                                // If the player was previously chosen
                                if (!goodChosen) {
                                    break;
                                }
                                // If it's a valid player, stop waiting for the Hoodlum
                                if (player.name.equals(server.gameActions.get(hoodlum.name))) {
                                    server.gameWaiting.replace(hoodlum.name, Boolean.FALSE);
                                    playerDeathWish[i] = player;
                                    break;
                                }
                            }
                        }
                        // If it was an invalid player, tell the cupid and wait again
                        if (playerDeathWish[i] == null) {
                            // If that player was not found, continue in the loop until they are
                            try {
                                hoodlum.output.writeObject("main.Player not found");
                                server.gameActions.replace(hoodlum.name, "");
                                playerDeathWish[i] = null;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            server.gameActions.replace(hoodlum.name, "");
                            try {
                                hoodlum.output.writeObject("Chosen " + (i+1) + ": " + playerDeathWish[i].name);
                            } catch(Exception e) {
                                System.out.println(e.getMessage());
                            }
                            break;
                        }
                    }
                }
            }
            // Stop the timer
            if(server.timers[2] > 0) {
                timer.interrupt();
            }

            try {
                Thread.sleep(3000);
                server.sendToAllPlayers("Hoodlum, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no hoodlum, wait a random amount of time so the other players don't realize there is no cupid
            // The random time is the configured time
            int randomWait = server.rand.nextInt(server.idleTimes[0]) + server.idleTimes[1];
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Hoodlum, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // This card only wakes up during the first night
    @Override
    public void nightWakeup() {
        return;
    }

    // This card does not have any checks to do after all of the deaths
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check method that will ensure there's only 1 Hoodlum card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Hoodlum")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Hoodlum card.");
        }
    }

    // The need to know method which tells the player all of their death wished players
    @Override
    public String needToKnow(Player player) {
        String result = "";

        // Find the Hoodlum card that the server uses
        HoodlumCard hoodlumCard = null;
        for(Card card : server.cards) {
            if(card instanceof HoodlumCard) {
                hoodlumCard = (HoodlumCard)card;
                break;
            }
        }

        // Check to make sure all the players have been chosen already
        for(Player deathWish : hoodlumCard.playerDeathWish) {
            if(deathWish == null) {
                result += "\nYou have chosen everyone yet!\n";
                return result;
            }
        }

        // Tell the player their chosen people
        result += "You want these people to die: " + Arrays.toString(hoodlumCard.playerDeathWish) + "\n";

        return result;
    }

    // The method that deals with the timer
    private synchronized void hoodlumTimerHelper(int time) {
        // Get an array of the cupid
        Player[] hoodlumArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Hoodlum")) {
                hoodlumArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, hoodlumArray);

        // If it gets here, that means the players ran out of time, so set all who haven't chosen to a random player
        // Get a list of all the players in the game
        HashSet<Player> potentials = new HashSet<Player>(server.currentPlayers);
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Hoodlum")) {
                potentials.remove(player);
                break;
            }
        }

        // Run through a loop for the amount of hoodlum players there needs to be
        for(int i = 0; i < server.werewolfNum; i++) {
            // If the first player is null, set one
            if (playerDeathWish[i] == null) {
                int random = server.rand.nextInt(potentials.size());
                Player player = (Player) potentials.toArray()[random];
                server.gameActions.replace(hoodlumArray[0].name, player.name);
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            // Remove the player set as the first linked
            potentials.remove(playerDeathWish[i]);
        }
    }
}
