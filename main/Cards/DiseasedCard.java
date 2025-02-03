package main.Cards;

import main.Player;
import main.WerewolfServer;

// The Diseased, which makes the werewolves unable to kill anyone the night after they kill the Diseased
public class DiseasedCard extends Card {
    // The flag to check if the ability has been used or not
    boolean diseasedAbility;

    // The constructor for the diseased
    public DiseasedCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Diseased";
        this.diseasedAbility = false;
    }

    // The help method for the diseased
    @Override
    public String help() {
        String result = "The Diseased is just like a normal villager, except they can help the villagers even in death. ";
        result += "If they are killed by the werewolves at night, then the next night the werewolves can't kill anyone. ";
        result += "This overrides the Wolf Cub's ability to increase the wolf kills once they die.";

        return result;
    }

    // The win condition for the Diseased, which is the same as the villager so it shouldn't get here
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

    // This card gets all of the people who are diseased
    @Override
    public void firstNightWakeup() {
        return;
    }

    // This card does not have any special night wakeups
    @Override
    public void nightWakeup() {
        return;
    }

    // The check after death method, which checks all the dead diseased and sees if they have restricted the werewolves'
    // kills or not after they died, and if they can
    @Override
    public void checkAfterDeaths() {
        // Check if the Diseased died during the night, which would mean that the diseasedAbility flag is true
        if(diseasedAbility) {
            server.werewolfKills = 0;

            // Reset it just in case the Doppelganger becomes the Diseased and dies again
            diseasedAbility = false;
        }
    }

    // This method makes sure there's only 1 diseased in the game
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Diseased")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Diseased card.");
        }
    }
}
