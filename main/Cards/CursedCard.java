package main.Cards;

import main.Player;
import main.WerewolfServer;

public class CursedCard extends Card {
    // Flag for if the Cursed has become a Werewolf. If it's 0, they're not a werewolf, if it's 1, they just turned into
    // a werewolf, if it's 2, they were already turned so don't tell them again
    int isWerewolf;

    // The constructor for the plain villager
    public CursedCard(WerewolfServer server) {
        this.server = server;
        this.team = "villager";
        this.cardName = "Cursed";
        this.nightWakeup = true;
        this.firstNightOnly = false;
        this.isWerewolf = 0;
        this.ranking = 3;
        this.winRank = 100;
    }

    @Override
    public String help() {
        String result = "The Cursed is a team switcher card, starting out as a villager and potentially ending up as ";
        result += "a werewolf by the end of the game. The Cursed starts out as a Villager and becomes a plain Werewolf ";
        result += "the next night after the werewolves attempt to kill them. So if the werewolves attempt to kill the ";
        result += "Cursed on the first night, they will still be a villager during the day, but they will be made aware ";
        result += "that they were attacked during the second night, in which case they then become a Werewolf for the rest ";
        result += "of the game. However, if the Bodyguard protects the Cursed on the same night the Werewolves attempt to ";
        result += "kill them, they do not turn.";

        return result;
    }

    // The won method for the Cursed. If they are a werewolf now, it does the werewolf win condition, and if not,
    // it does the villager win condition. Though, it should never actually get here
    @Override
    public boolean won() {
        if(isWerewolf >= 2) {
            // If the Cursed is a werewolf
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
        } else {
            // If the Cursed is not yet a werewolf
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
    }

    // The first night wakeup method, which returns since this doesn't do something special on the first night
    // It also doesn't call the normal night wakeup because there's no way it'll do something on this night.
    @Override
    public void firstNightWakeup() {
        return;
    }

    // The night wakeup method that checks if they had been targeted the previous night.
    // If so, they are now a werewolf, so tell them. If not, tell everyone it's checking anyway
    @Override
    public void nightWakeup() {
        // Tell everyone that the Cursed is waking up to see if they are a werewolf or not
        try {
            server.sendToAllPlayers("\nCursed, wake up and see if you have become a werewolf now.");
            server.sendToAllPlayers("If you were attacked by the werewolves the previous night then you ");
            server.sendToAllPlayers("are now a werewolf and your team and win conditions have changed.\n");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Check if there is a Cursed
        Player cursed = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cursed")) {
                cursed = player;
                cursed.tookNightAction = true;
                break;
            }
        }

        // If there's a cursed, do the proper things for it to set them as a Werewolf
        if(isWerewolf == 1 && cursed != null) {
            try {
                cursed.output.writeObject("You were attacked the previous night. You are now a Werewolf.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }

            // Do the required things for switching the Cursed to werewolf
            isWerewolf++;
            cursed.card.cardName += " -> Werewolf";
            cursed.card.team = "werewolf";
            cursed.card.isSeenAsWerewolf = true;

            // Check if there's a cupid, and if there is, call its cupid team assistance so that this new team change can be checked
            for(Card card : server.cards) {
                if(card instanceof CupidCard) {
                    ((CupidCard) card).cupidTeamAssistance(cursed);
                    break;
                }
            }

            // Tell everyone that the Cursed is going back to sleep
            try {
                Thread.sleep(3000);
                server.sendToAllPlayers("Cursed, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            try {
                Thread.sleep(3000);
                server.sendToAllPlayers("Cursed, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // This card has no check after death checks
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check method that makes sure there's only 1 of this card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Cursed")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Cursed card.");
        }
    }
}
