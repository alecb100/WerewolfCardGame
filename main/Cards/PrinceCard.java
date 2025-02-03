package main.Cards;

import main.Player;
import main.WerewolfServer;

// The Prince card, which if voted to die during the day, their card is revealed and they don't
public class PrinceCard extends Card {
    // Whether the ability has been used or not
    public boolean abilityUsed;

    // The constructor for the prince
    public PrinceCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Prince";
        this.abilityUsed = false;
    }

    // The help method for the Prince
    @Override
    public String help() {
        String result = "The Prince is a villager, and potentially a very important villager, because they are one of the ";
        result += "only roles that people are guaranteed to know. They are on the villager team but if they are voted to ";
        result += "be eliminated during the day, their card is revealed and they are not killed. However, if the villagers ";
        result += "vote to eliminate them after that, they do die.";

        return result;
    }

    // The win condition for the Prince, which is the same as the villager so it shouldn't get here
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

    // There is no first night wakeup for this card
    @Override
    public void firstNightWakeup() {
        return;
    }

    // There is no night wakeup for this card
    @Override
    public void nightWakeup() {
        return;
    }

    // Doesn't have a special check after death check
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // There can only be 1 prince card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Prince")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Prince card.");
        }
    }
}
