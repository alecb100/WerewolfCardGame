package main.Cards;

import main.Player;
import main.WerewolfServer;

// The Plain villager card, the most basic card in the game
public class VillagerCard extends Card {
    // The constructor for the plain villager
    public VillagerCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Villager";
        this.winRank = 50;
    }

    // The help method for the plain villager
    @Override
    public String help() {
        String result = "The plain Villager is the default card in the Werewolf game. Usually this card outnumbers all other cards. ";
        result += "Every day, the villagers wake up and try to find out which one of their neighbors are werewolves. At the end of every ";
        result += "day, they choose one person to kill. There are many other cards that are on the villager team, including the Seer, Hunter, etc.";
        result += " They win once they have killed all of the werewolves. There can still be people alive that are on the werewolf team (like the minion), but the actual ";
        result += "werewolves must be dead. These include the plain Werewolf, the Wolf Man, the Dire Wolf, and the Werewolf Cub.";
        return result;
    }

    // The win method for the plain villager
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

    // The plain villager does not wake up at night
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The plain villager does not wake up at night
    @Override
    public void nightWakeup() { return; }

    // The plain villager does not have any checks to do after the deaths
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // Doesn't have any preCheck
    @Override
    public void preCheck() {
        return;
    }
}