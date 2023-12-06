// The Cupid card, which has the ability to link 2 people together. Once one of them dies, the other dies too
public class CupidCard extends Card {
    Player[] linked;

    // The constructor for creating the Cupid
    public CupidCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 15;
        this.team = "cupid";
        this.cardName = "Cupid";
        this.winRank = 100;
        this.firstNightOnly = true;
        this.deathCheck = true;
    }

    // The help method for the Cupid
    @Override
    public String help() {
        String result = "The Cupid is one of the most interesting cards. They link 2 people together, can be themselves if they wish, ";
        result += "to be on a team together. They are put on their own team, the cupid team. The Cupid is also on this team. However, there's ";
        result += "a twist. If one of the linked people die, the other one dies as well. Those linked people know each other, but they do not ";
        result += "know who the Cupid is, nor do they know what cards each other are. This card overrides all other cards, including Tanner, ";
        result += "which means that the Tanner no longer wins if he dies. They still keep abilities they previously had, however, as this card ";
        result += "just overrides win conditions. The way these players win, including the Cupid, is if they are all still alive when another ";
        result += "team wins. Once another team wins, if the 2 linked people are still alive, they and the Cupid win as well!";

        return result;
    }

    // The win checking method for the Cupid. It's only called once another team won and checks to see if the linked people are still alive
    @Override
    public boolean won() {
        boolean hasCupid = false;
        Player cupid = null;
        // Checks if there is a cupid in the game
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                hasCupid = true;
                cupid = player;
                break;
            }
        }
        // Checks if there is one in the game but they're just dead
        if(!hasCupid) {
            for(Player player : server.dead) {
                if(player.card.cardName.contains("Cupid")) {
                    hasCupid = true;
                    cupid = player;
                    break;
                }
            }
        }

        // If there's a cupid, that means there are people who were linked
        if (hasCupid) {
            // If there are more than 3 players alive, that means it only got here because another card won
            boolean won = true;
            if(server.currentPlayers.size() > 3) {
                // Check if one of them are dead. If one is dead, they did not win
                for (int i = 0; i < 2; i++) {
                    if (linked[i].dead) {
                        won = false;
                        break;
                    }
                }

                // If the 2 linked people are alive, that means they won
                if (won) {
                    try {
                        // Tell everyone who was linked and who the Cupid is
                        server.sendToAllPlayers("The cupid team also won!");
                        server.sendToAllPlayers("The linked players were [" + linked[0].name + "," + linked[1].name + "] and the Cupid was " + cupid.name);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            } else {
                // If it got here, that means there are 3 players or fewer alive, which potentially could be all cupid team members
                for(Player player : server.currentPlayers) {
                    if(!player.card.team.contains("cupid")) {
                        won = false;
                        break;
                    }
                }
                if(won) {
                    for(Player player : server.currentPlayers) {
                        if(player.card.team.contains("cupid")) {
                            player.card.team = "cupid";
                        }
                    }
                }
            }
            return won;
        }
        // If there wasn't a Cupid then that means the non-existent Cupid didn't win
        return false;
    }

    // The Cupid only wakes up on the first night to link 2 people
    @Override
    public void firstNightWakeup() {
        // Tell everyone that the Cupid is waking up
        try {
            server.sendToAllPlayers("Cupid, wake up and choose 2 people to link. If one dies, the other dies. This is also ");
            server.sendToAllPlayers("your team, but they don't know who you are. You can choose yourself if you wish.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Checks if there is a Cupid
        Player cupid = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                cupid = player;
                try {
                    cupid.output.writeObject("Cupid, which 2 players do you wish to link? Put them each on separate lines please.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // If there is a Cupid, tell them to pick 2 players
        if(cupid != null) {
            linked = new Player[2];
            // Run through a 2 length loop of them picking people
            for (int i = 0; i < 2; i++) {
                server.gameWaiting.replace(cupid.name, Boolean.TRUE);
                while(true) {
                    // If they made an action
                    if (!server.gameActions.get(cupid.name).equals("")) {
                        // Run through the players currently alive and make sure it is a valid player
                        for (Player player : server.currentPlayers) {
                            // If the player was previously chosen, don't allow them to be the next linked
                            if(i == 1 && server.gameActions.get(cupid.name).equals(linked[0].name)) {
                                System.out.println("duplicate");
                                break;
                            }
                            // If it's a valid player, stop waiting for the Cupid
                            if (player.name.equals(server.gameActions.get(cupid.name))) {
                                server.gameWaiting.replace(cupid.name, Boolean.FALSE);
                                linked[i] = player;
                                break;
                            }
                        }
                        // If it was an invalid player, tell the cupid and wait again
                        if (linked[i] == null) {
                            // If that player was not found, continue in the loop until they are
                            try {
                                cupid.output.writeObject("Player not found");
                                server.gameActions.replace(cupid.name, "");
                                linked[i] = null;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            server.gameActions.replace(cupid.name, "");
                            try {
                                cupid.output.writeObject("Linked " + (i+1) + ": " + linked[i].name);
                            } catch(Exception e) {
                                System.out.println(e.getMessage());
                            }
                            break;
                        }
                    }
                }
            }

            // Tell everyone the Cupid is falling back to sleep and that the 2 linked are waking up to see each other.
            try {
                server.sendToAllPlayers("Cupid, go back to sleep.\n");
                server.sendToAllPlayers("The 2 linked people, wake up and see who the other is. You are now on a team with ");
                server.sendToAllPlayers("each other and the Cupid. If either of you dies, the other dies as well. Your win conditions ");
                server.sendToAllPlayers("are also overridden. You now only win once another team wins and both of you are still alive.");
                for(int i = 0; i < 2; i++) {
                    // Tell each linked who they're linked to
                    linked[i].output.writeObject("\nYou are linked to: " + linked[1 - i].name + "\n");
                    linked[i].card.team += " - cupid";
                }
                Thread.sleep(3000);
                // Tell everyone that the linked are going back to sleep
                server.sendToAllPlayers("Linked, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no cupid, wait a random amount of time so the other players don't realize there is no cupid
            int randomWait = (int)(Math.random() * 5000) + 6000;
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Cupid, go back to sleep.\n");
                server.sendToAllPlayers("The 2 linked people, wake up and see who the other is. You are now on a team with ");
                server.sendToAllPlayers("each other and the Cupid. If either of you dies, the other dies as well. Your win conditions ");
                server.sendToAllPlayers("are also overridden. You now only win once another team wins and both of you are still alive.");
                Thread.sleep(3000);
                server.sendToAllPlayers("Linked, go back to sleep.");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The Cupid only wakes up the first night. After that they don't wake up anymore
    @Override
    public void nightWakeup() {
        return;
    }

    // The Cupid has a check after deaths check since there are linked players. It checks if one of the linked died, and kills the other
    @Override
    public void checkAfterDeaths() {
        // Check to see if there's a Cupid in the game
        boolean hasCupid = false;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                hasCupid = true;
                break;
            }
        }
        // Check if the Cupid is dead. If they are, there was a Cupid
        if(!hasCupid) {
            for(Player player : server.dead) {
                if(player.card.cardName.contains("Cupid")) {
                    hasCupid = true;
                    break;
                }
            }
        }

        // Check if the first linked is dead but the second is alive
        if(linked[0].dead && !linked[1].dead) {
            try {
                // Tell everyone that the second linked died and what their card was
                server.sendToAllPlayers("\n" + linked[1].name + " has been killed!\nThey were " + linked[1].card.cardName + "!\n");

                // Set the second linked to dead
                server.currentPlayers.remove(linked[1]);
                server.dead.add(linked[1]);
                linked[1].dead = true;

                // Tell that player they are dead
                linked[1].output.writeObject("!!!!!YOU DIED!!!!!");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else if(linked[1].dead && !linked[0].dead) {
            // Check if the second linked is dead but the first is alive
            try {
                // Tell everyone that the first linked died and what their card was
                server.sendToAllPlayers("\n" + linked[0].name + " has been killed!\nThey were " + linked[0].card.cardName + "!\n");

                // Set the second linked to dead
                server.currentPlayers.remove(linked[0]);
                server.dead.add(linked[0]);
                linked[0].dead = true;

                // Tell that player they are dead
                linked[0].output.writeObject("!!!!!YOU DIED!!!!!");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
