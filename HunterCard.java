// The Hunter is like the normal villager, except when they die they bring someone with them
public class HunterCard extends Card {
    // The flag to check if they chose someone to die with them
    boolean bringWith;

    // The constructor for the hunter
    public HunterCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Hunter";
        this.bringWith = false;
    }

    // The help method for the Hunter
    @Override
    public String help() {
        String result = "The Hunter is just like a normal villager, with the same win condition and everything. The only ";
        result += "difference is that when they die, regardless of when they die, they get to bring someone to the afterlife ";
        result += "with them, regardless of who it is.";

        return result;
    }

    // The win condition for the Hunter, which is the same for the villager so it shouldn't get here
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

    // The Hunter does not have any first night wakeups
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The Hunter does not have any night wakeups
    @Override
    public void nightWakeup() {
        return;
    }

    @Override
    public void checkAfterDeaths() {
        // Check all hunters, and if they died, ask them who they want to bring with them
        for(Player hunter : server.dead) {
            if(hunter.card.cardName.contains("Hunter") && !((HunterCard)hunter.card).bringWith) {
                // Tell the server to wait for the hunter
                server.gameWaiting.replace(hunter.name, Boolean.TRUE);
                try {
                    server.sendToAllPlayers("\nSince they are a hunter, they will bring someone to the afterlife!");
                    hunter.output.writeObject("You died. Who do you want to die with you?\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                // Tell their hunter card they are no longer able to bring someone with them
                HunterCard card = ((HunterCard)hunter.card);
                card.bringWith = true;

                // Ask the Hunter who they want to bring with them
                Player choice = null;

                // Create the thread for the timer
                Thread timer = null;
                if(server.timers[2] > 0) {
                    timer = new Thread(() -> hunterTimerHelper(server.timers[2], hunter));
                    timer.start();
                }

                // Continue in this while loop until they choose someone
                while(true) {
                    if(!server.gameActions.get(hunter.name).equals("")) {
                        // Run through the players currently alive and make sure it is a valid player
                        for(Player player : server.currentPlayers) {
                            // If it's a valid player, stop waiting for the hunter
                            if(player.name.equals(server.gameActions.get(hunter.name))) {
                                choice = player;
                                server.gameWaiting.replace(hunter.name, Boolean.FALSE);
                                try {
                                    hunter.output.writeObject("You killed " + choice.name + "\n");
                                } catch(Exception e) {
                                    System.out.println(e.getMessage());
                                }
                                break;
                            }
                        }
                        // If it was a valid player, tell the hunter their choice and have them die
                        if(choice != null) {
                            try {
                                // Stop the timer
                                if(server.timers[2] > 0) {
                                    timer.interrupt();
                                }

                                Thread.sleep(3000);
                                server.sendToAllPlayers("\n" + choice.name + " has been chosen to be killed!\nThey were " + choice.card.cardName + "!\n");
                                choice.dead = true;
                                server.dead.add(choice);
                                server.currentPlayers.remove(choice);
                                choice.output.writeObject("!!!!!YOU DIED!!!!!");
                                server.deathCheckNum++;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                            return;
                        } else {
                            // If that player was not found, continue in the loop until they are
                            try {
                                hunter.output.writeObject("Player not found");
                                server.gameActions.replace(hunter.name, "");
                            } catch(Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    // The Hunter does not have any pre checks
    @Override
    public void preCheck() {
        return;
    }

    // The method that deals with the timer
    private synchronized void hunterTimerHelper(int time, Player hunter) {
        // Get an array of the hunter
        Player[] hunterArray = new Player[1];
        hunterArray[0] = hunter;

        // Call the timer method for the alive players and the time given
        server.timer(time, hunterArray);

        // If it gets here, that means the players ran out of time, so set all who haven't chosen to a random player
        int random = server.rand.nextInt(server.currentPlayers.size());
        server.gameActions.replace(hunter.name, server.currentPlayers.toArray()[random].toString());
        try {
            Thread.sleep(500);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
