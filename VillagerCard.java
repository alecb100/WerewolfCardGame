public class VillagerCard extends Card {
    public VillagerCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = false;
        this.ranking = 50;
        this.team = "villager";
        this.cardName = "Plain Villager";
        this.winRank = 50;
    }

    @Override
    public boolean won() {
        boolean oneVillager = false;
        // Checking if there is at least 1 villager alive
        for(Player player : server.currentPlayers) {
            if(player.card.team.equals("villager")) {
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
            if(!player.dead && player.card.team.equals("werewolf")) {
                result = false;
                break;
            }
        }

        return result;
    }

    @Override
    public void firstNightWakeup() {
        return;
    }

    @Override
    public void nightWakeup() { return; }
}
