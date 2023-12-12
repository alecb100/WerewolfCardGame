import java.util.HashSet;

// The Mason card, which all Masons wake up the first night and see who the other Masons are
public class MasonCard extends Card {
    // The constructor for the mason
    public MasonCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Mason";
        this.nightWakeup = true;
        this.firstNightOnly = true;
        this.ranking = 25;
    }

    @Override
    public String help() {
        String result = "The Masons are just like normal villagers. The only difference is that they wake up on the first ";
        result += "night to see who the other Masons are. The Masons are on the villager team, so the people you see are ";
        result += "your allies!";

        return result;
    }

    // The win condition of the Masons, which is the same as the plain villager, so it shouldn't get here
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

    // The Masons all wake up on the first night and see who the other Masons are
    @Override
    public void firstNightWakeup() {
        try {
            // Tell everyone that the masons are waking up
            server.sendToAllPlayers("\nMasons, wake up and see who your other fellow Masons are. Remember, they are on your team!\n");
            HashSet<Player> masons = new HashSet<Player>();
            // Find out who all the masons are
            for(Player player : server.currentPlayers) {
                if(player.card.cardName.contains("Mason")) {
                    masons.add(player);
                }
            }
            // Tell all the Masons who each other are
            for(Player mason : masons) {
                mason.output.writeObject("\nThe Masons are: " + masons + "\n");
            }
            // Wait, and then tell everyone the minion is going back to sleep
            Thread.sleep(3000);
            server.sendToAllPlayers("Masons, go back to sleep.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // The Masons don't have a special night wakeup
    @Override
    public void nightWakeup() {
        return;
    }

    // There are no special checks after death or the Masons
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check for the Masons, which checks to see if there are at least 2 of these cards
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Mason")) {
                cards++;
            }
        }
        if(cards < 2) {
            throw new IllegalArgumentException("If the Mason card is included, there needs to be at least 2.");
        }
    }
}
