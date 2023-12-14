import java.util.HashSet;

// The Dire Wolf card. They are a werewolf that chooses a companion to link themselves to
public class DireWolfCard extends Card {
    // The player that the Dire Wolf is linked to
    Player companion;

    // Whether the Dire Wolf has been killed as a result of their link being dead
    // This is required because of the Doppelganger becomes this card and the link is already dead,
    // it turns them into a normal werewolf rather than a Dire Wolf, to which they would die immediately
    boolean hasDied;

    // The constructor for creating the Werewolf
    public DireWolfCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 46;
        this.team = "werewolf";
        this.cardName = "Dire Wolf";
        this.firstNightOnly = true;
        this.isSeenAsWerewolf = true;
        this.hasDied = false;
    }

    // The help method for the Dire Wolf
    @Override
    public String help() {
        String result = "The Dire Wolf is a minor werewolf. They are on the werewolves team and act exactly like a normal ";
        result += "werewolf. The only difference is that they pick a companion on the first night. If that companion dies, ";
        result += "they die. However, if the Dire Wolf dies, the companion is fine. The companion also doesn't know they are ";
        result += "a companion, and the other wolves don't know who the Dire Wolf's companion is.\nAn important note, if the ";
        result += "Doppelganger, or a similar card, becomes the Dire Wolf, if the companion is still alive, they will be under ";
        result += "the same rules. But if the companion is already dead, they will be just like a normal werewolf.";

        return result;
    }

    // The Dire Wolf won method is the same as the normal werewolf
    @Override
    public boolean won() {
        int werewolfCount = 0;
        int otherCount = 0;
        // Goes through all the players and counts how many werewolf team members there are and how many
        // villager team members there are
        for(Player player : server.currentPlayers) {
            if(server.checkWerewolf(player) || player.card.team.contains("werewolf")) {
                werewolfCount++;
            } else {
                otherCount++;
            }
        }
        // Checks whether the amount of alive werewolves is greater than or equal to the amount of alive villagers
        return werewolfCount >= otherCount;
    }

    @Override
    public void firstNightWakeup() {
        // Tell everyone that the Dire Wolf is waking up
        try {
            server.sendToAllPlayers("\nDire Wolf, wake up and determine who you want as your companion.");
            server.sendToAllPlayers("If they die, you die too, but not vice versa. They also don't know they're your companion.");
            server.sendToAllPlayers("You cannot choose a fellow wolf.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        // Determine who the Dire Wolf is. If this card is selected, there will always be a Dire Wolf
        Player direWolf = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Dire Wolf")) {
                direWolf = player;
                server.gameWaiting.replace(player.name, Boolean.TRUE);
                try {
                    direWolf.output.writeObject("Who do you want to choose as your companion?");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // Get all the valid players to choose (all players that aren't werewolves)
        HashSet<Player> choices = new HashSet<Player>();
        for(Player player : server.currentPlayers) {
            if(!server.checkWerewolf(player)) {
                choices.add(player);
            }
        }

        // Wait for the player to choose someone
        Player choice = null;

        // Create the thread for the timer
        Thread timer = null;
        if(server.timers[2] > 0) {
            timer = new Thread(() -> direWolfTimerHelper(server.timers[2]));
            timer.start();
        }

        // Continue in this while loop until they choose someone
        while(true) {
            if(!server.gameActions.get(direWolf.name).equals("")) {
                // Run through the players currently alive and make sure it is a valid player
                for(Player player : choices) {
                    // If it's a valid player, stop waiting for the Dire Wolf
                    if(player.name.equals(server.gameActions.get(direWolf.name))) {
                        choice = player;
                        server.gameWaiting.replace(direWolf.name, Boolean.FALSE);
                        break;
                    }
                }
                // If it was a valid player, tell the doppelganger their choice and save it
                if(choice != null) {
                    // Stop the timer
                    if(server.timers[2] > 0) {
                        timer.interrupt();
                    }
                    try {
                        direWolf.output.writeObject("Your companion: " + choice.name);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    companion = choice;
                    try {
                        Thread.sleep(3000);
                        server.sendToAllPlayers("Dire Wolf, go back to sleep.");
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                    return;
                } else {
                    // If that player was not found, continue in the loop until they are
                    try {
                        direWolf.output.writeObject("Player not found");
                        server.gameActions.replace(direWolf.name, "");
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    }

    // The Dire Wolf only wakes up on the first night (apart from the normal werewolf wake ups)
    @Override
    public void nightWakeup() {
        return;
    }

    // The method to check if their companion died. If they did, the Dire Wolf dies too
    @Override
    public void checkAfterDeaths() {
        // If the Dire Wolf hasn't been set to dead by this method yet and their companion is dead
        if(!hasDied && companion.dead) {
            // Set hasDied to true
            hasDied = true;
            // Find the Dire Wolf in the list of alive players
            for(Player player : server.currentPlayers) {
                if(player.card.cardName.contains("Dire Wolf")) {
                    try {
                        // Tell everyone that the Dire Wolf died and what their card was
                        server.sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!\n");

                        // Set the Dire Wolf to dead
                        server.currentPlayers.remove(player);
                        server.dead.add(player);
                        player.dead = true;

                        // Tell that player they are dead
                        player.output.writeObject("!!!!!YOU DIED!!!!!");

                        // Increment death check num
                        server.deathCheckNum++;

                        return;
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    }

    // The pre check method that makes sure there's only 1 of this card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Dire Wolf")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Dire Wolf card.");
        }
    }

    // This card has information the Dire Wolf needs to know, which is who their companion is
    @Override
    public String needToKnow(Player player) {
        String result = "";

        // Find the Dire Wolf card that the server is using
        DireWolfCard direWolfCard = null;
        for(Card card : server.cards) {
            if(card instanceof DireWolfCard) {
                direWolfCard = (DireWolfCard)card;
                break;
            }
        }

        // Check if the player hasn't chosen a companion yet
        if(direWolfCard.companion == null) {
            result += "You haven't chosen your companion yet!\n";
            return result;
        }

        // If the companion has already died from their companion dying, tell them
        if(direWolfCard.hasDied) {
            result += "Your companion has already died.";
            // If the player asking isn't only the Dire Wolf (which means they are likely the Doppelganger)
            if(!player.card.cardName.equals("Dire Wolf") && player.card.cardName.contains("Dire Wolf")) {
                // If the companion is already dead, then they are just a normal Werewolf (will only get here if Doppelganger)
                result += " This means that you are basically a normal Werewolf.";
            }
        } else {
            // If not, tell them who their companion is
            result += "Your companion is: " + direWolfCard.companion.name;
        }

        result += "\n";

        return result;
    }

    // The method that deals with the timer
    private synchronized void direWolfTimerHelper(int time) {
        // Get an array of the Dire Wolf
        Player[] direWolfArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Dire Wolf")) {
                direWolfArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, direWolfArray);

        // If it gets here, that means the players ran out of time, so set all who haven't chosen to a random player
        HashSet<Player> potentials = new HashSet<Player>(server.currentPlayers);
        for(Player player : server.currentPlayers) {
            if(server.checkWerewolf(player)) {
                potentials.remove(player);
            }
        }

        // Set the companion as a random player
        if(companion == null) {
            int random = server.rand.nextInt(potentials.size());
            server.gameActions.replace(direWolfArray[0].name, potentials.toArray()[random].toString());
            try {
                Thread.sleep(500);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
