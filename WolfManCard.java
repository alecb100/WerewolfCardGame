public class WolfManCard extends Card {

    // The constructor for creating the Werewolf
    public WolfManCard(WerewolfServer server) {
        this.server = server;
        this.team = "werewolf";
        this.cardName = "Wolf Man";
        this.isSeenAsWerewolf = false;
    }

    @Override
    public String help() {
        String result = "The Wolf Man is the same as a normal werewolf. The only difference is that a seer does not see ";
        result += "them as a werewolf.";

        return result;
    }

    // The win condition for the Wolf Man, which is the same as the werewolf
    @Override
    public boolean won() {
        int werewolfCount = 0;
        int otherCount = 0;
        // Goes through all the players and counts how many werewolf team members there are and how many
        // villager team members there are
        for(Player player : server.currentPlayers) {
            if(server.checkWerewolf(player) || player.card.team.contains("werewolf")) {
                werewolfCount++;
            } else {
                otherCount++;
            }
        }
        // Checks whether the amount of alive werewolves is greater than or equal to the amount of alive villagers
        return werewolfCount >= otherCount;
    }

    // The Wolf Man does not have any special first night wakeups
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The Wolf Man does not have any night wakeup
    @Override
    public void nightWakeup() {
        return;
    }

    // There are no check after deaths for the Wolf Man
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // There are no pre checks for the Wolf Man
    @Override
    public void preCheck() {
        return;
    }
}
