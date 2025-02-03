package main.Cards;

import main.Player;
import main.WerewolfServer;

// The Lycan is just like a normal villager, except the seer sees them as a werewolf
public class LycanCard extends Card {
    // The constructor for the Lycan
    public LycanCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Lycan";
        this.isSeenAsWerewolf = true;
    }

    // The help method for the Lycan
    @Override
    public String help() {
        String result = "The Lycan is just like a villager, except that the Seer, and any similar card, sees the Lycan ";
        result += "as a werewolf, despite being just like a villager. They win the same way normal villagers win, except ";
        result += "for that one change.";

        return result;
    }

    // The win condition for Lycan, which is the same as villager, though it should never get here
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

    // The Lycan has no first night wakeup
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The Lycan has no night wakeup
    @Override
    public void nightWakeup() {
        return;
    }

    // The Lycan has no checks done after deaths
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The Lycan has no pre check
    @Override
    public void preCheck() {
        return;
    }
}
