import java.util.HashSet;

// The Apprentice Seer, who becomes the Seer when one dies
public class ApprenticeSeerCard extends Card {
    // The flag to see if the Apprentice Seer will become the seer
    int promotion;

    // The constructor for the Apprentice Seer
    public ApprenticeSeerCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Apprentice seer";
        this.ranking = 19;
        this.promotion = 0;
        this.nightWakeup = true;
        this.firstNightOnly = false;
    }

    // The help method for the card
    @Override
    public String help() {
        String result = "The Apprentice Seer is not a seer yet, they are a Seer in training. They are on the villagers ";
        result += "team and will win when they do. Their ability comes into play when a Seer dies. When a Seer dies, they ";
        result += "get promoted and become a full Seer. Only then will they be able to see if someone is a werewolf at night.";

        return result;
    }

    // The win condition, which is the same as the Villager, so it shouldn't get here
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

    // There is no first night wakeup for this card
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The night wakeup, which sets this card to the seer if it has seen that one has died
    @Override
    public void nightWakeup() {
        try {
            // Tell everyone that the apprentice seer is waking up
            server.sendToAllPlayers("\nApprentice Seer, wake up to see if a Seer has died so you can be promoted.");
            // Find out who the apprentice is
            if(promotion == 1) {
                promotion = 2;
                for (Player player : server.currentPlayers) {
                    if (player.card.cardName.contains("Apprentice seer")) {
                        player.output.writeObject("\nThe Seer has died, so you have been promoted. ");
                        player.output.writeObject("You will wake up with the other Seers to try and see the werewolves.");
                        player.card.cardName += " -> Seer";
                        break;
                    }
                }
            } else if(promotion == 0){
                for(Player player : server.currentPlayers) {
                    if(player.card.cardName.contains("Apprentice seer")) {
                        player.output.writeObject("\nThe Seer has not died, you are not promoted yet.");
                        break;
                    }
                }
            }
            // Wait, and then tell everyone the minion is going back to sleep
            Thread.sleep(3000);
            server.sendToAllPlayers("\nApprentice Seer, go back to sleep.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // The check after death method for this card, which checks if the seer died to see if they can become the Seer next
    // night
    @Override
    public void checkAfterDeaths() {
        for(Player player : server.dead) {
            if(player.card.cardName.contains("Seer") && promotion == 0) {
                promotion = 1;
                break;
            }
        }
    }

    // The pre check for this card, which makes sure there's only 1 of it and also that there's at least 1 Seer
    @Override
    public void preCheck() {
        int cards = 0;
        int cards2 = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Apprentice seer")) {
                cards++;
            } else if(card.cardName.equals("Seer")) {
                cards2++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Apprentice seer card.");
        }
        if(cards2 < 1) {
            throw new IllegalArgumentException("If an Apprentice Seer is in the game, there must be at least 1 Seer.");
        }
    }
}
