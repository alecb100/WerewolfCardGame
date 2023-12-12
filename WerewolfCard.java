import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

// The main Werewolf card, the main werewolf enemy in the game
public class WerewolfCard extends Card {
    // A HashMap of the werewolves and their votes to kill
    HashMap<Player, Player> werewolves;

    // A boolean for whether all the players have chosen a person to kill or not
    boolean ultraGood;

    // The constructor for creating the Werewolf
    public WerewolfCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 45;
        this.team = "werewolf";
        this.cardName = "Werewolf";
        this.winRank = 5;
        this.firstNightOnly = false;
        this.isSeenAsWerewolf = true;
    }

    // Its help method, detailing what it does at night and how it wins
    @Override
    public String help() {
        String result = "This is the plain Werewolf, the main werewolf of the game. They wake up every night and choose one person to kill (doing that every night). ";
        result += "They win by eliminating enough villagers so that the amount of alive werewolves or their sympathizers equal or is greater than the ";
        result += "amount of alive villagers. There are other werewolves in the game, including the Dire Wolf, the Wolf Man, and the Werewolf Cub.";
        result += " They also have helpers, such as the minion, who do not actually count as werewolves but are on the werewolves team.";
        return result;
    }

    // The win method that checks if the werewolves won
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

    // This card does not have a special first night wakeup
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    // During the night, the werewolves all choose 1 person to kill collectively, every night
    @Override
    public void nightWakeup() {
        // See how many werewolves there are
        werewolves = new HashMap<Player, Player>();
        for (Player player : server.currentPlayers) {
            if(server.checkWerewolf(player)) {
                werewolves.put(player, null);
            }
        }

        // Tell everyone that werewolves are waking up
        try {
            server.sendToAllPlayers("\nAll werewolves, wake up, and determine who you want to kill.\nAll werewolves must vote. The player with the most votes will be killed.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Tell the werewolves who the other werewolves are, but not what kind of werewolf they are
        try {
            outputPrint("Werewolves: " + werewolves.keySet().toString());
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Wait for each werewolves' choice of kill
        for(Player player : werewolves.keySet()) {
            server.gameWaiting.replace(player.name, Boolean.TRUE);
        }

        // Run through this loop until all werewolves have chosen someone they want to kill for the amount of kills they can make
        for(Player player : werewolves.keySet()) {
            try {
                // Check if there is more than 0 kills that must be made
                if(server.werewolfKills != 0) {
                    player.output.writeObject("You must kill " + server.werewolfKills + ".\n");
                } else {
                    // If there are 0, that means they killed the Diseased the last night
                    player.output.writeObject("Because you killed the Diseased the last night, you are too sick to kill anyone this night.\n");
                    Thread.sleep(3000);
                }
            } catch(Exception e) {
                System.out.println();
            }
        }

        for(int j = 0; j < server.werewolfKills; j++) {
            new Thread(this::sendToWerewolves).start();
            for(Player player : werewolves.keySet()) {
                try {
                    player.output.writeObject("Who is your kill #" + (j+1) + "?");
                    server.gameWaiting.replace(player.name, Boolean.TRUE);
                    werewolves.replace(player, null);
                } catch(Exception e) {
                    System.out.println();
                }
            }
            while(true) {

                // Set flags for running through the loop and determining if it should be over
                ultraGood = false;
                boolean good = true;

                // Create a hashMap to count the amount of votes each player has
                HashMap<Player, Integer> count = new HashMap<Player, Integer>();
                for(Player player : server.currentPlayers) {
                    // Put all non-werewolves into this HashMap
                    if(!server.checkWerewolf(player)) {
                        count.put(player, 0);
                    }
                }

                // Run through the werewolves' votes and talley up the corresponding player
                for(Player player : werewolves.values()) {
                    if(player != null) {
                        count.replace(player, count.get(player)+1);
                    } else {
                        // If even one of the werewolves hasn't voted yet, continue and retry the loop
                        good = false;
                        break;
                    }
                }
                if(!good) {
                    continue;
                }

                // If it got here, that means all the werewolves have voted, so stop looking for their votes
                ultraGood = true;
                server.stopWaiting();
                // Have the program sleep so the other threads can catch up
                try {
                    Thread.sleep(3000);
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

                // Find out who the player with the most votes is for the number of werewolf kills they need to make
                int highest = -1;
                Player dead = null;
                for (Player player : count.keySet()) {
                    if (count.get(player) > highest) {
                        highest = count.get(player);
                        dead = player;
                    }
                }

                // Check if they are a Cursed, and if so, don't kill them, but set them to a werewolf for the next night
                if (dead.card.cardName.contains("Cursed")) {
                    for (Card card : server.cards) {
                        if (card.cardName.contains("Cursed")) {
                            ((CursedCard) card).isWerewolf++;
                        }
                    }
                } else {
                    // Set their dead flag to true (not put them in the dead HashSet, so that the program knows who's newly dead)
                    // if they are not Cursed
                    dead.dead = true;

                    // Check if that player was Diseased, and if they were, set the diseased ability to true so they can't
                    // kill anyone the following night
                    if(dead.card.cardName.contains("Diseased")) {
                        for (Card card : server.cards) {
                            if (card instanceof DiseasedCard diseasedCard) {
                                diseasedCard.diseasedAbility = true;
                            }
                        }
                    }
                }
                // Print this for logging purposes
                System.out.println("Chosen kill: " + dead.name);
                for (Player player : werewolves.keySet()) {
                    try {
                        player.output.writeObject("chosen kill: " + dead.name + "\n");
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                break;
            }
        }

        try {
            // Tell all the players that the werewolves are going back to sleep
            server.sendToAllPlayers("All werewolves, go back to sleep.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // A helper method to easily send each werewolf their bretheren's choice, once each new choice is made
    private void sendToWerewolves() {
        // Get the names of all the possible players that could be chosen
        String[] names = new String[server.currentPlayers.size()-werewolves.size()];
        int i = 0;
        // Add each player, making sure they don't have their tower active and are not a werewolf
        for(Player player : server.currentPlayers) {
            if(!server.checkWerewolf(player) && !player.dead && !player.tower) {
                names[i] = player.name;
                i++;
            }
        }
        // Run through this loop until all werewolves have made their choice
        while(!ultraGood) {
            // Continuously check the werewolves' choices
            for(Player player : werewolves.keySet()) {
                // If they made a valid choice
                if(!server.gameActions.get(player.name).equals("") && Arrays.asList(names).contains(server.gameActions.get(player.name))) {
                    try {
                        // Tell all other werewolves of that werewolves' choice
                        outputPrint(player.name + " voted: " + server.gameActions.get(player.name));
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                    // Set the werewolves HashMap of that werewolves' choice
                    werewolves.replace(player, server.players.get(server.gameActions.get(player.name)));
                    // Reset the gameActions HashMap so that the players aren't told of the others' votes infinitely
                    server.gameActions.replace(player.name, "");
                } else if(!server.gameActions.get(player.name).equals("") && !Arrays.asList(names).contains(server.gameActions.get(player.name))) {
                    // If the name wasn't found, tell that werewolf and that werewolf only that it's not a valid player
                    try {
                        player.output.writeObject("Not a valid player");
                        server.gameActions.replace(player.name, "");
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    }

    // A helper method to send text to all werewolves
    private void outputPrint(String s) throws IOException {
        for(Player player : werewolves.keySet()) {
            player.output.writeObject(s);
            player.output.flush();
        }
    }

    // There is no special checkAfterDeath for normal werewolves
    @Override
    public void checkAfterDeaths() { return; }

    // Checks to see if there are more non-werewolf cards than werewolf cards so that the game doesn't instantly end
    @Override
    public void preCheck() {
        int werewolfCards = 0;
        int otherCards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Werewolf") || card.cardName.equals("Wolf Man") || card.cardName.equals("Wolf Cub") || card.cardName.equals("Dire Wolf")) {
                werewolfCards++;
            } else {
                otherCards++;
            }
        }
        if(werewolfCards >= otherCards) {
            throw new IllegalArgumentException("There has to be more non-werewolf cards than werewolf cards.");
        }
    }
}