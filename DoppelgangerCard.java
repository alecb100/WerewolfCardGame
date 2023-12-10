public class DoppelgangerCard extends Card {
    // Chosen player that the Doppelganger will take the role of
    Player chosenPlayer;

    // Whether the Doppelganger became that person or not. 0 means haven't switched, 1 means just switched, 2 means already switched
    int hasSwitched;

    // The constructor for creating the Doppelganger
    public DoppelgangerCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 2;
        this.team = "villager";
        this.cardName = "Doppelganger";
        this.firstNightOnly = false;
        this.winRank = 100;
        this.hasSwitched = 0;
        this.deathCheckRank = 100;
    }

    @Override
    public String help() {
        String result = "The Doppelganger is one of the more interesting cards. The Doppelganger picks a player in the ";
        result += "first night to take the role of once they die. They become their role the next night after death, not immediately ";
        result += "after. It can be any role, including the Drunk or Cursed. What's important to know is that the Doppelganger's team ";
        result += "and win conditions will change to whatever role they take, and they don't know the player's role until after ";
        result += "they die and become them. Before this happens, the Doppelganger is a normal villager. Additionally, if ";
        result += "the player had a first night action or an ability they already used, they are unable to use it again. ";
        result += "This means if they become the Minion, they are on the werewolves team but do not know who the werewolves ";
        result += "are, so effectively playing as the Doppelganger requires the player to have been paying close attention ";
        result += "the whole game.\nAn important note, for cards that have links with other players, like the Dire Wolf, ";
        result += "if the Dire Wolf-like card dies but their link is still alive, the Doppelganger will still have that link, ";
        result += "but if they die because their link died, they will basically be just a normal werewolf.";

        return result;
    }

    // The win method that checks if they won. If they haven't switched yet, it calls the plain villager win condition.
    // If they have, it calls the chosen player's win condition. However, it should never give true because the winRank
    // is always much lower than all the other cards that truly do have win conditions, including the real chosen
    // player's card.
    @Override
    public boolean won() {
        if(hasSwitched == 0 || hasSwitched == 1) {
            // Hasn't switched so do the plain villager win condition. If it's 0, it hasn't switched yet and if it's 1,
            // it hasn't notified the player that they switched yet (ie it hasn't gotten to the next night yet).
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
        } else {
            // Has switched so do the chosen player's win condition
            return chosenPlayer.card.won();
        }
    }

    // The first night wakeup which has the Doppelganger pick someone they want to become
    @Override
    public void firstNightWakeup() {
        // Tell everyone that the doppelganger is waking up
        try {
            server.sendToAllPlayers("Doppelganger, wake up and determine who you want to become after they die.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine if there is a doppelganger or not (there would only ever be 1)
        Player doppelganger = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Doppelganger")) {
                doppelganger = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    doppelganger.output.writeObject("Who do you want to become after they die?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // If there is a doppelganger, wait for the player to choose someone
        if(doppelganger != null) {
            Player choice = null;
            // Continue in this while loop until they choose someone
            while(true) {
                if(!server.gameActions.get(doppelganger.name).equals("")) {
                    // Run through the players currently alive and make sure it is a valid player
                    for(Player player : server.currentPlayers) {
                        // If it's a valid player, stop waiting for the doppelganger
                        if(player.name.equals(server.gameActions.get(doppelganger.name))) {
                            choice = player;
                            server.gameWaiting.replace(doppelganger.name, Boolean.FALSE);
                            break;
                        }
                    }
                    // If it was a valid player, tell the doppelganger their choice and save it
                    if(choice != null) {
                        try {
                            doppelganger.output.writeObject("Player to become: " + choice.name);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        chosenPlayer = choice;
                        try {
                            Thread.sleep(1000);
                            server.sendToAllPlayers("Doppelganger, go back to sleep.");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                        return;
                    } else {
                        // If that player was not found, continue in the loop until they are
                        try {
                            doppelganger.output.writeObject("Player not found");
                            server.gameActions.replace(doppelganger.name, "");
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        } else {
            // If there is no bodyguard, wait a random amount of time so the other players don't realize there is no bodyguard
            int randomWait = server.rand.nextInt(5000) + 5000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Doppelganger, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The night wakeup which tells everyone it checks if they have become a different role
    @Override
    public void nightWakeup() {
        // Determine if the doppelganger is alive
        Player doppelganger = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Doppelganger")) {
                doppelganger = player;
                break;
            }
        }

        if(hasSwitched == 1 && doppelganger != null) {
            try {
                server.sendToAllPlayers("\nDoppelganger, wake up. If the person you chose is now dead, you become their role.");
                server.sendToAllPlayers("If they had a first night action or an ability they use once and already use it,");
                server.sendToAllPlayers("you can't access it, sorry. Your team has switched to that team and same with your");
                server.sendToAllPlayers("win condition. Good luck!\n");
                doppelganger.output.writeObject("Your new card is: " + chosenPlayer.card.cardName);
                hasSwitched++;
                doppelganger.card.cardName += " -> " + chosenPlayer.card.cardName;
                doppelganger.card.team = chosenPlayer.card.team;
                doppelganger.card.isSeenAsWerewolf = chosenPlayer.card.isSeenAsWerewolf;

                // Check if there's a cupid, and if there is, call its cupid team assistance so that this new team change can be checked
                for(Card card : server.cards) {
                    if(card instanceof CupidCard) {
                        ((CupidCard) card).cupidTeamAssistance(doppelganger);
                        break;
                    }
                }

                Thread.sleep(3000);
                server.sendToAllPlayers("Doppelganger, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // The doppelganger either doesn't exist, is dead, or hasn't changed yet. Regardless, continue like normally
            try {
                server.sendToAllPlayers("\nDoppelganger, wake up. If the person you chose is now dead, you become their role.");
                server.sendToAllPlayers("If they had a first night action or an ability they use once and already use it,");
                server.sendToAllPlayers("you can't access it, sorry. Your team has switched to that team and same with your");
                server.sendToAllPlayers("win condition. Good luck!\n");
                Thread.sleep(3000);
                server.sendToAllPlayers("Doppelganger, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The check after deaths check. It checks if the chosen player has been eliminated and tells the card to switch
    // the Doppelganger to it if they haven't already
    @Override
    public void checkAfterDeaths() {
        if(chosenPlayer.dead && hasSwitched == 0) {
            hasSwitched++;
        }
    }

    // The pre check method that makes sure there's only 1 of this card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Doppelganger")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Doppelganger card.");
        }
    }
}
