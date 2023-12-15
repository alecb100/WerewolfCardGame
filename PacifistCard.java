// The Pacifist card, someone who can only vote for no one
public class PacifistCard extends Card {

    // The constructor for the pacifist
    public PacifistCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Pacifist";
    }

    // The help method for the pacifist
    @Override
    public String help() {
        String result = "The Pacifist is exactly like a normal Villager. The only difference is during the day they cannot ";
        result += "vote for a player, they must vote for no one.";

        return result;
    }

    // The win method, which is the same as the villager, so it shouldn't get here
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

    // The pacifist doesn't have a first night wakeup
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The Pacifist doesn't have any night wakeups
    @Override
    public void nightWakeup() {
        return;
    }

    // The Pacifist doesn't have any check after deaths
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The Pacifist doesn't have any prechecks
    @Override
    public void preCheck() {
        return;
    }
}
