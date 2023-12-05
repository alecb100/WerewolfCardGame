// The class for the Tanner card, which is suicidal
public class TannerCard extends Card {

    // The constructor for the Tanner, which is just like the Villager except is on its own team and is always the first
    // to be checked for wins
    public TannerCard(WerewolfServer server) {
        this.server = server;
        this.team = "tanner";
        this.cardName = "Tanner";
        this.winRank = 1;
    }

    // The help method for the Tanner card
    @Override
    public String help() {
        String result = "The Tanner card is the card that wishes to die. They win instantly if they are killed by any means. ";
        result += "They may use any means necessary to get someone to kill them. Either make people think they're a werewolf, ";
        result += "become unreliable so it seems they are a werewolf, etc. As long as they die, they win";

        return result;
    }

    // The won method for the Tanner card
    @Override
    public boolean won() {
        // Checks if an alive player has been set to dead. This is only checked after all night wake-ups, so nothing
        // would make the card no longer dead
        for(Player player : server.currentPlayers) {
            if(player.card.team.equals("tanner") && player.dead) {
                return true;
            }
        }
        // Checks if the player is in the dead HashMap, which means that they were recently put there
        for(Player player : server.dead) {
            if(player.card.team.equals("tanner")) {
                return true;
            }
        }
        return false;
    }

    // The Tanner does not wake up at night
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The Tanner does not wake up at night
    @Override
    public void nightWakeup() {
        return;
    }

    // The Tanner does not have a special thing it does after it dies, except that it won
    @Override
    public void checkAfterDeaths() {
        return;
    }
}
