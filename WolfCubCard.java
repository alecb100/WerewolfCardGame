// The Wolf Cub is just like a normal werewolf, except the night after it dies, the werewolves can kill 1 additional player
public class WolfCubCard extends Card {
    // Whether the death ability already added an additional werewolf kill
    boolean deathAddition;

    // The constructor for creating the Werewolf
    public WolfCubCard(WerewolfServer server) {
        this.server = server;
        this.team = "werewolf";
        this.cardName = "Wolf Cub";
        this.isSeenAsWerewolf = true;
        this.deathAddition = false;
    }

    // The help method for Wolf Cub
    @Override
    public String help() {
        String result = "The Wolf Cub is very similar to a normal werewolf. The only difference is that when they are killed, ";
        result += "the wolves the next night are able to kill an additional person for that night. Apart from that, they have ";
        result += "the same win conditions as a normal werewolf.";

        return result;
    }

    // The win condition for wolf cub, which is the same as werewolf so it shouldn't get here
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


    // The first night wakeup method for wolf cub, which it doesn't have one so it just returns
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The night wakeup for wolf cub, which it doesn't have so it just returns
    @Override
    public void nightWakeup() {
        return;
    }

    // The Wolf Cub check after deaths which adds an additional kill for the werewolves the next night
    @Override
    public void checkAfterDeaths() {
        // Check all the werewolf cubs. If one is dead, add another kill to the amount of kills the werewolves can do
        for(Player player : server.dead) {
            if(player.card.cardName.contains("Wolf Cub") && !((WolfCubCard)(player.card)).deathAddition) {
                // Check if the werewolf kills for the next night are 0 already, and if they are that means they
                // killed the Diseased the previous night, so the werewolf kills may not be updated
                if(server.werewolfKills != 0) {
                    server.werewolfKills++;
                }
                ((WolfCubCard)(player.card)).deathAddition = true;
            }
        }
    }

    // There are no pre checks for the Wolf Cub
    @Override
    public void preCheck() {
        return;
    }
}
