package main.Cards;

import main.Player;
import main.WerewolfServer;

import java.util.concurrent.ConcurrentHashMap;

// The Tough Guy card, who doesn't die until the next night, but only if targetted by the wolves
public class ToughGuyCard extends Card {
    // The flag to see if they have been targeted
    ConcurrentHashMap<Player, Integer> targeted;

    // The constructor for the tough guy
    public ToughGuyCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Tough Guy";
        this.targeted = new ConcurrentHashMap<>();
        this.ranking = -1;
        this.nightWakeup = true;
        this.firstNightOnly = true;
    }

    // The help method for the tough guy
    @Override
    public String help() {
        String result = "The Tough Guy is an interesting card. They are on the villagers team, but the special thing ";
        result += "about them is if they are targeted by the werewolves at night, they don't die until the next night. ";
        result += "This only applies to being targeted by the wolves. If they are targeted by any other role that night, ";
        result += "they die like normal.";

        return result;
    }

    // the win condition for the tough guy, which is the same as the villager, so it shouldn't get here
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

    // This card checks through all the players, seeing if any are tough guys and putting them in the targeted hash map
    @Override
    public void firstNightWakeup() {
        // Check through all the players to see which are tough guys
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Tough Guy")) {
                targeted.put(player, 0);
            }
        }
    }

    // There's no night wakeup for this card
    @Override
    public void nightWakeup() {
        return;
    }

    // The check after deaths, which checks if the tough guys are supposed to die this night
    @Override
    public void checkAfterDeaths() {
        for(Player player : server.currentPlayers) {
            if(targeted.containsKey(player)) {
                if (targeted.get(player) == 2 && server.isNight) {
                    // If they are supposed to die this night
                    try {
                        // Tell everyone that the second linked died and what their card was
                        server.sendToAllPlayers("\n" + player.name + " has been killed!\nThey were " + player.card.cardName + "!\n");

                        // Set the player to dead
                        server.currentPlayers.remove(player);
                        server.dead.add(player);
                        player.dead = true;

                        // Tell that player they are dead
                        player.output.writeObject("!!!!!YOU DIED!!!!!");

                        // Increment death check num
                        server.deathCheckNum++;
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                } else if (targeted.get(player) == 1) {
                    // If they are already set to die, make sure the server knows that for next night
                    targeted.replace(player, 2);
                }
            }
        }
    }

    // There's no pre check for this card
    @Override
    public void preCheck() {
        return;
    }
}
