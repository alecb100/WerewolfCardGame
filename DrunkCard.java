import java.util.HashSet;

public class DrunkCard extends Card {
    // Chosen card that the Drunk will become
    Card chosenCard;

    // Count the amount of nights
    int nightCount;

    // The constructor for creating the Drunk
    public DrunkCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 5;
        this.team = "villager";
        this.cardName = "Drunk";
        this.firstNightOnly = false;
        this.preCheckRank = 50;
        this.nightCount = 0;
        this.winRank = 100;
    }

    @Override
    public String help() {
        String result = "The Drunk is one of the more interesting and complicated roles. The drunk is essentially a ";
        result += "normal villager for the first 2 days, being on the villager team, having the same win condition, etc. ";
        result += "Once the third night hits, they sober up and become their true role. Their true role is not revealed to ";
        result += "them until this point. At which point, their team switches and they are able to use any abilities that new ";
        result += "card has. However, if that card had an action they performed on the very first night, they do not get to use ";
        result += "it. But from hence forth, they are that role and will wake up during the night whenever that role would. ";
        result += "There are very few roles that this card cannot be, and that is the Dire Wolf and Hoodlum. Because both of ";
        result += "those cards had first night actions that would result in something very important, like their win condition ";
        result += "or a death effect, they cannot become those cards. They can, however, become a werewolf if there's more than 1 ";
        result += "that was specified in the game.\nIt is important to note, cards that override other win conditions, like the cupid ";
        result += "will be updated and still override it. Additionally, the seer and any other similar cards see the Drunk as their ";
        result += "true card. So, while the Drunk is on the villagers' team while drunk, the seer will see if they are really a ";
        result += "werewolf or not, even before the Drunk knows.";

        return result;
    }

    @Override
    public boolean won() {
        // If the night is 3 or after, that means the drunk is now their new card so call that card's won check
        if(nightCount >= 3) {
            return chosenCard.won();
        } else {
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

    // The Drunk does not have a special first night wakeup, except to set their isSeenAsWerewolf to their card's
    @Override
    public void firstNightWakeup() {
        // Find out if there is a drunk
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Drunk")) {
                // Set their isSeenAsWerewolf flag to the chosen card's
                player.card.isSeenAsWerewolf = chosenCard.isSeenAsWerewolf;
            }
        }
        nightWakeup();
    }

    // Every night, count which night it is. If it's the 3rd, the Drunk is no longer drunk
    @Override
    public void nightWakeup() {
        // Count every night
        nightCount++;

        // If it's the 3rd, the Drunk finally sobers up
        if(nightCount == 3) {
            // Determine if there's an alive drunk
            Player drunk = null;
            for(Player player : server.currentPlayers) {
                if(player.card.cardName.contains("Drunk")) {
                    drunk = player;
                    break;
                }
            }

            if(drunk != null) {
                try {
                    server.sendToAllPlayers("\nDrunk, wake up. It is now the 3rd night and you are now sober.");
                    server.sendToAllPlayers("You will now receive a new card that may change your team and win condition.");
                    server.sendToAllPlayers("If you received a card that had a first night action, maybe you shouldn't have been drunk.");
                    server.sendToAllPlayers("Good luck!\n");
                    drunk.output.writeObject("Your new card is: " + chosenCard.cardName);
                    drunk.card.cardName += " -> " + chosenCard.cardName;
                    drunk.card.team = chosenCard.team;

                    // Check if the chosen card is Tough Guy, and if it is, add them to the tough guy hashMap
                    if(chosenCard instanceof ToughGuyCard) {
                        for(Card card : server.cards) {
                            if(card instanceof ToughGuyCard toughGuyCard) {
                                toughGuyCard.targeted.put(drunk, 0);
                                break;
                            }
                        }
                    }

                    // Check if there's a cupid, and if there is, call its cupid team assistance so that this new team change can be checked
                    for(Card card : server.cards) {
                        if(card instanceof CupidCard) {
                            ((CupidCard) card).cupidTeamAssistance(drunk);
                            break;
                        }
                    }

                    Thread.sleep(3000);
                    server.sendToAllPlayers("Drunk, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                // The drunk either doesn't exist or is dead. Regardless, continue as if they are alive
                try {
                    server.sendToAllPlayers("\nDrunk, wake up. It is now the 3rd night and you are now sober.");
                    server.sendToAllPlayers("You will now receive a new card that may change your team and win condition.");
                    server.sendToAllPlayers("If you received a card that had a first night action, maybe you shouldn't have been drunk.");
                    server.sendToAllPlayers("Good luck!\n");
                    Thread.sleep(3000);
                    server.sendToAllPlayers("Drunk, go back to sleep.\n");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    // The drunk has no special check after death checks
    @Override
    public void checkAfterDeaths() {
        return;
    }

    // The pre check method that makes sure there's only 1 of this card
    @Override
    public void preCheck() {
        // Verifies that there is only 1 drunk card
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Drunk")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Drunk card.");
        }

        // Checks that there is an extra card for the Drunk to choose
        if(server.chooseCards.size() < server.currentPlayers.size() + 1) {
            throw new IllegalArgumentException("There must be at least 1 more card than there are players because of the Drunk card.");
        }

        // If there is only 1 werewolf, the Drunk cannot choose that card
        int wolfCount = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Werewolf") || card.cardName.equals("Wolf Cub") || card.cardName.equals("Wolf Man") || card.cardName.equals("Dire Wolf")) {
                wolfCount++;
            }
            if(wolfCount > 1) {
                break;
            }
        }

        // Takes out all the cards that the Drunk cannot be, like the Dire Wolf, Hoodlum, or the only werewolf if there's only 1
        // Also take out the drunk card (this card) so that it can't choose itself
        HashSet<Card> extraCards = new HashSet<Card>();
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Dire Wolf") || card.cardName.equals("Hoodlum") || (wolfCount == 1 &&
                    (card.cardName.equals("Werewolf") || card.cardName.equals("Wolf Man") || card.cardName.equals("Wolf Cub"))) ||
                    card.cardName.equals("Drunk")) {
                extraCards.add(card);
            }
        }
        server.chooseCards.removeAll(extraCards);

        // Choose the random card and remove it from chooseCards, so it can't be assigned to someone else
        int randomCard = server.rand.nextInt(server.chooseCards.size());
        int i = 0;
        for(Card card : server.chooseCards) {
            if(i == randomCard) {
                chosenCard = card;
                server.chooseCards.remove(card);
                break;
            }
            i++;
        }

        // Add back all of those extra cards
        server.chooseCards.addAll(extraCards);
    }

    // The need to know method for Drunk. It will tell the player the need to know stuff for its new card, if it has switched
    @Override
    public String needToKnow(Player player) {
        String result = "";

        // Find the card the server uses for Drunk
        DrunkCard drunkCard = null;
        for(Card card : server.cards) {
            if(card instanceof DrunkCard) {
                drunkCard = (DrunkCard)card;
                break;
            }
        }

        if(drunkCard.nightCount < 3) {
            result += "\nYou haven't sobered up yet. Sit tight, there's only been " + drunkCard.nightCount + " nights. ";
            result += "You will switch on the 3rd night.\n";
        } else {
            result += "\nYou have become the " + drunkCard.chosenCard.cardName + ".";
            result += drunkCard.chosenCard.needToKnow(player) + "\n";
        }

        return result;
    }
}
