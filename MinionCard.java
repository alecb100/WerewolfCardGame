import java.util.HashSet;

// The Minion card is the helper to the Werewolves. His job is to be a meat shield for the werewolves pretty much
public class MinionCard extends Card {
    // The constructor for creating the Minion
    public MinionCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 47;
        this.team = "werewolf";
        this.cardName = "Minion";
        this.firstNightOnly = true;
    }

    // The help method for the Minion
    @Override
    public String help() {
        String result = "The Minion is on the werewolf team. Their only special trait is that they know who all of the ";
        result += "werewolves are, but they don't know who he is. His whole goal is to take suspicion away from the ";
        result += "werewolves so that his team can win. That may include pulling suspicion over to himself.";

        return result;
    }

    // The win condition of the minion, which is the same as the werewolf win condition
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

    // The first night wakeup method, which just tells the minion who the werewolves are
    @Override
    public void firstNightWakeup() {
        try {
            // Tell everyone that the minion is waking up
            server.sendToAllPlayers("Minion, wake up and see who the werewolves are. Remember, they don't know who you are.");
            HashSet<Player> minions = new HashSet<Player>();
            HashSet<Player> werewolves = new HashSet<Player>();
            // Find out who all the werewolves are and who the minion is
            for(Player player : server.currentPlayers) {
                if(player.card.cardName.contains("Minion")) {
                    minions.add(player);
                } else if(server.checkWerewolf(player)) {
                    werewolves.add(player);
                }
            }
            // If there's a minion, tell them who the werewolves are
            for(Player minion : minions) {
                minion.output.writeObject("\nThe werewolves are: " + werewolves.toString());
                if(minions.size() > 1) {
                    minion.output.writeObject("\nAll of the minions are: " + minions.toString());
                }
            }
            // Wait, and then tell everyone the minion is going back to sleep
            Thread.sleep(3000);
            server.sendToAllPlayers("\nMinion, go back to sleep.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // The minion only wakes up the first night
    @Override
    public void nightWakeup() {
        return;
    }

    // The minion doesn't have any after deaths check
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // There is no preCheck for this card
    @Override
    public void preCheck() {
        return;
    }
}
