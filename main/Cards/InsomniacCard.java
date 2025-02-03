package main.Cards;

import main.Player;
import main.WerewolfServer;

import java.util.HashSet;

// The insomniac, who can tell if one of their neighbors took some sort of night action
public class InsomniacCard extends Card {
    // The constructor for the insomniac
    public InsomniacCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Insomniac";
        this.nightWakeup = true;
        this.firstNightOnly = false;
        this.ranking = 105;
    }

    // The help method
    @Override
    public String help() {
        String result = "The Insomniac is just like the normal villager. The only difference is they are able to see if ";
        result += "one of their neighbors woke up for any reason. They don't know which one woke up and they don't know ";
        result += "if they woke up and took some sort of action or just woke up to see something.";

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

    // There are no special first night wakeup, so it calls the nightWakeup method
    @Override
    public void firstNightWakeup() {
        nightWakeup();
    }

    // The Insomniac wakes to see if one of their neighbors took a night action or not
    @Override
    public void nightWakeup() {
        try {
            // Tell everyone that the minion is waking up
            server.sendToAllPlayers("\nInsomniac, wake up and see if either of your neighbors took a night action.");
            HashSet<Player> insomniacs = new HashSet<Player>();
            // Find out who all the werewolves are and who the minion is
            for(Player player : server.currentPlayers) {
                if(player.card.cardName.contains("Insomniac")) {
                    insomniacs.add(player);
                    player.tookNightAction = true;
                }
            }
            // Tell all the insomniacs who the others are, if there's more than 1
            if(insomniacs.size() > 1) {
                for (Player insomniac : insomniacs) {
                    insomniac.output.writeObject("\nAll of the insomniacs are: " + insomniacs.toString());
                }
            }
            // If there's an insomniac, tell them if a neighbor took a night action
            for(Player insomniac : insomniacs) {
                Player[] insomniacNeighbors = server.neighborHelper(insomniac);
                if(insomniacNeighbors[0].tookNightAction || insomniacNeighbors[1].tookNightAction) {
                    if(insomniacs.size() > 1) {
                        for (Player insomniac2 : insomniacs) {
                            insomniac2.output.writeObject("At least one of " + insomniac.name + "'s neighbors took a night action!");
                        }
                    } else {
                        insomniac.output.writeObject("At least one of your neighbors took a night action!");
                    }
                } else {
                    if(insomniacs.size() > 1) {
                        for (Player insomniac2 : insomniacs) {
                            insomniac2.output.writeObject("None of " + insomniac.name + "'s neighbors took a night action!");
                        }
                    } else {
                        insomniac.output.writeObject("None of your neighbors took a night action!");
                    }
                }
            }
            // Wait, and then tell everyone the minion is going back to sleep
            Thread.sleep(3000);
            server.sendToAllPlayers("\nInsomniac, go back to sleep.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // There is no special check after death for this card
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // This method checks to make sure neighbors is set correctly
    @Override
    public void preCheck() {
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
}
