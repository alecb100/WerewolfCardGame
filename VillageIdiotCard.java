// The Village Idiot card, they cannot vote for no one and MUST vote for someone to kill every day
public class VillageIdiotCard extends Card {

    // The constructor for the village idiot
    public VillageIdiotCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Village Idiot";
    }
    @Override
    public String help() {
        String result = "The Village Idiot is the same as a normal villager, except that they must vote for someone during ";
        result += "the day. They are unable to vote for no one.";

        return result;
    }

    // The win condition, which is the same as the villager, so it shouldn't get here
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

    // This role does not have any night wakeups
    @Override
    public void firstNightWakeup() {
        return;
    }

    // This role does not have any night wakeups
    @Override
    public void nightWakeup() {
        return;
    }

    // This role does not have any checks after death
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // This role does not have any prechecks
    @Override
    public void preCheck() {
        return;
    }
}
