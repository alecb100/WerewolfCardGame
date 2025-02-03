package main.Cards;

import main.Player;
import main.WerewolfServer;

import java.util.HashSet;

// The Cupid card, which has the ability to link 2 people together. Once one of them dies, the other dies too
public class CupidCard extends Card {
    public Player[] linked;

    // The constructor for creating the Cupid
    public CupidCard(WerewolfServer server) {
        this.server = server;
        this.nightWakeup = true;
        this.ranking = 15;
        this.team = "villager";
        this.cardName = "Cupid";
        this.firstNightOnly = true;
    }

    // The help method for the Cupid
    @Override
    public String help() {
        String result = "The Cupid card is one of the cooler cards, but is confusing to understand. The cupid ";
        result += "wakes up the first night and chooses 2 people to link together (they can choose themselves if they ";
        result += "want). Once one of them dies, the other also dies, and cannot be prevented. They both know who they ";
        result += "are linked to, but they don't know what their card is. Depending on their card, they may have switched ";
        result += "to a different team. If they were both on the same team, they remain on that team. If one was a villager ";
        result += "and the other was on the werewolf team but NOT a werewolf (like a minion), they are both on the villager ";
        result += "team. In any other circumstance, they are on their own team and can only win if they are the last people ";
        result += "alive. It is more than likely that these 2 are on the villager team as there are always more villagers ";
        result += "than other cards, so it's in the cupid's best interest not to say who they linked (because they are always ";
        result += "on the villager team, unless they chose to link themselves) as the werewolves would go after them if they ";
        result += "both weren't werewolves. However, they may not be on the villager team, so it is ultimately up to the ";
        result += "Cupid to reveal them or not.\n\n";
        result += "Here are the conditions for team switching:\n";
        result += "\t1.\tIf both were on the same team, they remain on the same team.\n";
        result += "\t2.\tIf one was a villager and the other on the werewolf team but NOT a werewolf, they are both on the ";
        result += "villager team.\n\t3.\tIn any other circumstance, they are on their own team and will only win if they ";
        result += "are the only ones to remain.\n\nThe cupid is not affected by the linked people, but this overrides all ";
        result += "other win conditions.";

        return result;
    }

    @Override
    public boolean won() {
        // Check to see if there's a cupid
        boolean hasCupid = false;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                hasCupid = true;
                break;
            }
        }
        // Check if they are dead, but still was in the game
        if(!hasCupid) {
            for(Player player : server.dead){
                if(player.card.cardName.contains("Cupid")) {
                    hasCupid = true;
                    break;
                }
            }
        }
        // If there was never a cupid, quit out because no reason to check
        if(!hasCupid) {
            return false;
        }

        if(team.equals("villager")) {
            // If both the linked people are villagers
            boolean oneVillager = false;
            // Checking if there is at least 1 villager alive
            for (Player player : server.currentPlayers) {
                if (!player.card.team.equals("werewolf")) {
                    oneVillager = true;
                    break;
                }
            }
            if (!oneVillager) {
                return false;
            }

            // Checking to make sure all werewolves are dead
            boolean result = true;
            for (Player player : server.currentPlayers) {
                if (!player.dead && server.checkWerewolf(player)) {
                    result = false;
                    break;
                }
            }

            return result;
        } else if(team.equals("werewolf")) {
            // If both the linked people are werewolves
            int werewolfCount = 0;
            int otherCount = 0;
            // Goes through all the players and counts how many werewolf team members there are and how many
            // villager team members there are
            for(Player player : server.currentPlayers) {
                if(server.checkWerewolf(player) || player.card.team.equals("werewolf")) {
                    werewolfCount++;
                } else {
                    otherCount++;
                }
            }
            // Checks whether the amount of alive werewolves is greater than or equal to the amount of alive villagers
            return werewolfCount >= otherCount;
        } else {
            // If the linked people were put on their own team, check to see if those 2 are the last ones alive
            if(server.currentPlayers.size() != 2) {
                return false;
            }
            boolean won = true;
            for(Player player : server.currentPlayers) {
                if(!player.card.team.equals("cupid")) {
                    won = false;
                    break;
                }
            }
            return won;
        }
    }

    // The Cupid only wakes up on the first night to link 2 people
    @Override
    public void firstNightWakeup() {
        // Tell everyone that the Cupid is waking up
        try {
            server.sendToAllPlayers("Cupid, wake up and choose 2 people to link. If one dies, the other dies. This is also ");
            server.sendToAllPlayers("your team, but they don't know who you are. You can choose yourself if you wish.");
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        // Checks if there is a Cupid
        Player cupid = null;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                cupid = player;
                try {
                    cupid.output.writeObject("\nCupid, which 2 players do you wish to link? Put them each on separate lines please.");
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            }
        }

        // If there is a Cupid, tell them to pick 2 players
        if(cupid != null) {
            linked = new Player[2];

            // Create the thread for the timer
            Thread timer = null;
            if(server.timers[2] > 0) {
                timer = new Thread(() -> cupidTimerHelper(server.timers[2] + 10000));
                timer.start();
            }

            // Run through a 2 length loop of them picking people
            for (int i = 0; i < 2; i++) {
                server.gameWaiting.replace(cupid.name, Boolean.TRUE);
                cupid.tookNightAction = true;
                while(true) {
                    // If they made an action
                    if (!server.gameActions.get(cupid.name).equals("")) {
                        // Run through the players currently alive and make sure it is a valid player
                        for (Player player : server.currentPlayers) {
                            // If the player was previously chosen, don't allow them to be the next linked
                            if(i == 1 && server.gameActions.get(cupid.name).equals(linked[0].name)) {
                                System.out.println("duplicate");
                                break;
                            }
                            // If it's a valid player, stop waiting for the Cupid
                            if (player.name.equals(server.gameActions.get(cupid.name))) {
                                server.gameWaiting.replace(cupid.name, Boolean.FALSE);
                                linked[i] = player;
                                break;
                            }
                        }
                        // If it was an invalid player, tell the cupid and wait again
                        if (linked[i] == null) {
                            // If that player was not found, continue in the loop until they are
                            try {
                                cupid.output.writeObject("main.Player not found");
                                server.gameActions.replace(cupid.name, "");
                                linked[i] = null;
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            server.gameActions.replace(cupid.name, "");
                            try {
                                cupid.output.writeObject("Linked " + (i+1) + ": " + linked[i].name);
                            } catch(Exception e) {
                                System.out.println(e.getMessage());
                            }
                            break;
                        }
                    }
                }
            }
            // Stop the timer
            if(server.timers[2] > 0) {
                timer.interrupt();
            }

            // Tell everyone the Cupid is falling back to sleep and that the 2 linked are waking up to see each other.
            try {
                Thread.sleep(3000);
                server.sendToAllPlayers("Cupid, go back to sleep.\n");
                server.sendToAllPlayers("The 2 linked people, wake up and see who the other is. If one of you dies, the ");
                server.sendToAllPlayers("other also dies and cannot be prevented. Your win conditions may have also been ");
                server.sendToAllPlayers("overridden. It depends on what teams each of you were on before. Good luck!");
                for(int i = 0; i < 2; i++) {
                    // Tell each linked who they're linked to
                    linked[i].output.writeObject("\nYou are linked to: " + linked[1 - i].name + "\n");
                }
                // Check if they are on the same team
                if(linked[0].card.team.equals(linked[1].card.team)) {
                    // If they are both on the same team
                    this.team = linked[0].card.team;
                } else if((linked[0].card.team.equals("villager") && linked[1].card.team.equals("werewolf") && !server.checkWerewolf(linked[1])) ||
                        (linked[1].card.team.equals("villager") && linked[0].card.team.equals("werewolf") && !server.checkWerewolf(linked[0]))) {
                    // If one is a villager and the other is on the werewolf team but NOT a werewolf
                    linked[0].card.team = "villager";
                    linked[1].card.team = "villager";
                } else {
                    // If they are none of the above, they are on their own team
                    linked[0].card.team = "cupid";
                    linked[1].card.team = "cupid";
                    this.team = "cupid";
                    this.winRank = 0;
                }
                Thread.sleep(3000);
                // Tell everyone that the linked are going back to sleep
                server.sendToAllPlayers("\nLinked, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            // If there is no cupid, wait a random amount of time so the other players don't realize there is no cupid
            // Random time is between the given idle times
            int randomWait = server.rand.nextInt(server.idleTimes[0]) + server.idleTimes[1];
            try {
                Thread.sleep(randomWait);
                server.sendToAllPlayers("Cupid, go back to sleep.\n");
                server.sendToAllPlayers("The 2 linked people, wake up and see who the other is. If one of you dies, the ");
                server.sendToAllPlayers("other also dies and cannot be prevented. Your win conditions may have also been ");
                server.sendToAllPlayers("overridden. It depends on what teams each of you were on before. Good luck!");
                Thread.sleep(3000);
                server.sendToAllPlayers("\nLinked, go back to sleep.\n");
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The Cupid only wakes up the first night. After that they don't wake up anymore
    @Override
    public void nightWakeup() {
        return;
    }

    // The Cupid has a check after deaths check since there are linked players. It checks if one of the linked died, and kills the other
    @Override
    public void checkAfterDeaths() {
        // Check to see if there's a Cupid in the game
        boolean hasCupid = false;
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                hasCupid = true;
                break;
            }
        }
        // Check if the Cupid is dead. If they are, there was a Cupid
        if(!hasCupid) {
            for(Player player : server.dead) {
                if(player.card.cardName.contains("Cupid")) {
                    hasCupid = true;
                    break;
                }
            }
        }

        // Check if the first linked is dead but the second is alive
        if(linked[0].dead && !linked[1].dead) {
            try {
                // Tell everyone that the second linked died and what their card was
                server.sendToAllPlayers("\n" + linked[1].name + " has been killed!\nThey were " + linked[1].card.cardName + "!\n");

                // Set the second linked to dead
                server.currentPlayers.remove(linked[1]);
                server.dead.add(linked[1]);
                linked[1].dead = true;

                // Tell that player they are dead
                linked[1].output.writeObject("!!!!!YOU DIED!!!!!");

                // Increment death check num
                server.deathCheckNum++;
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        } else if(linked[1].dead && !linked[0].dead) {
            // Check if the second linked is dead but the first is alive
            try {
                // Tell everyone that the first linked died and what their card was
                server.sendToAllPlayers("\n" + linked[0].name + " has been killed!\nThey were " + linked[0].card.cardName + "!\n");

                // Set the second linked to dead
                server.currentPlayers.remove(linked[0]);
                server.dead.add(linked[0]);
                linked[0].dead = true;

                // Tell that player they are dead
                linked[0].output.writeObject("!!!!!YOU DIED!!!!!");

                // Increment death check num
                server.deathCheckNum++;
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // The pre check method that makes sure there's only 1 of this card
    @Override
    public void preCheck() {
        int cards = 0;
        for(Card card : server.chooseCards) {
            if(card.cardName.equals("Cupid")) {
                cards++;
            }
        }
        if(cards > 1) {
            throw new IllegalArgumentException("There can't be more than 1 Cupid card.");
        }
    }

    // A helper method to update a team switcher's team based on Cupid stuff, if they are linked
    public void cupidTeamAssistance(Player teamSwitcher) {
        // Check if the teamSwitcher is already on the cupid team, and if it is, set their team correctly
        // This only happens if they switched cards to a player that was linked and put on the cupid team, meaning
        // that this player wouldn't still be cupid in all but the scenario that they swapped with their cupid mate,
        // but in that case, they would also be dead
        if(teamSwitcher.card.team.equals("cupid")) {
            // Get the name of the last card they are (could be that a doppelganger chose a cursed and was cursed after becoming them,
            // in this case their card name is Doppelganger -> Cursed -> Werewolf, othersCard is going to be Werewolf at the end)
            String switchersCard = teamSwitcher.card.cardName;
            while(switchersCard.indexOf('>') != -1) {
                switchersCard = switchersCard.substring(switchersCard.indexOf('>') + 2);
            }

            // Set their actual team to that card's team
            for(Card card : server.cards) {
                if(card.cardName.contains(switchersCard)) {
                    teamSwitcher.card.team = card.team;
                    break;
                }
            }

            // If the team is still cupid, set it to villager
            if(teamSwitcher.card.team.equals("cupid")) {
                teamSwitcher.card.team = "villager";
            }
        }

        // Check if the teamSwitcher was linked, and if they're not, return as there's nothing that needs to be done
        if(!linked[0].equals(teamSwitcher) && !linked[1].equals(teamSwitcher)) {
            return;
        }

        // If they were, set the other linked person's team back to what it was originally and do the checks again
        this.team = "villager";
        this.winRank = 100;
        int otherIndex;

        // Figure out which one is the other one
        if(linked[0].equals(teamSwitcher)) {
            otherIndex = 1;
        } else {
            otherIndex = 0;
        }

        // Get the name of the last card they are (could be that a doppelganger chose a cursed and was cursed after becoming them,
        // in this case their card name is Doppelganger -> Cursed -> Werewolf, othersCard is going to be Werewolf at the end)
        String othersCard = linked[otherIndex].card.cardName;
        while(othersCard.indexOf('>') != -1) {
            othersCard = othersCard.substring(othersCard.indexOf('>') + 2);
        }

        // Find that original card and set the other's team to that
        for(Card card : server.cards) {
            if (card.cardName.equals(othersCard)) {
                linked[otherIndex].card.team = card.team;
                break;
            }
        }

        // Check if they are on the same team
        if(linked[0].card.team.equals(linked[1].card.team)) {
            // If they are both on the same team
            this.team = linked[0].card.team;
        } else if((linked[0].card.team.equals("villager") && linked[1].card.team.equals("werewolf") && !server.checkWerewolf(linked[1])) ||
                (linked[1].card.team.equals("villager") && linked[0].card.team.equals("werewolf") && !server.checkWerewolf(linked[0]))) {
            // If one is a villager and the other is on the werewolf team but NOT a werewolf
            linked[0].card.team = "villager";
            linked[1].card.team = "villager";
        } else {
            // If they are none of the above, they are on their own team
            linked[0].card.team = "cupid";
            linked[1].card.team = "cupid";
            this.team = "cupid";
            this.winRank = 0;
        }
    }

    // The method that deals with the timer
    private synchronized void cupidTimerHelper(int time) {
        // Get an array of the cupid
        Player[] cupidArray = new Player[1];
        for(Player player : server.currentPlayers) {
            if(player.card.cardName.contains("Cupid")) {
                cupidArray[0] = player;
                break;
            }
        }

        // Call the timer method for the alive players and the time given
        server.timer(time, cupidArray);

        // If it gets here, that means the players ran out of time, so set all who haven't chosen to a random player
        // Get a list of all the players in the game
        HashSet<Player> potentials = new HashSet<Player>(server.currentPlayers);

        // If the first player is null, set one
        if(linked[0] == null) {
            int random = server.rand.nextInt(potentials.size());
            Player player = (Player) potentials.toArray()[random];
            server.gameActions.replace(cupidArray[0].name, player.name);
            try {
                Thread.sleep(500);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }

        // Remove the player set as the first linked
        potentials.remove(linked[0]);

        // If the second linked is null, pick a random player
        if(linked[1] == null) {
            int random = server.rand.nextInt(potentials.size());
            Player player = (Player) potentials.toArray()[random];
            server.gameActions.replace(cupidArray[0].name, player.name);
            try {
                Thread.sleep(500);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}