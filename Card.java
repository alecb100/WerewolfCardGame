public abstract class Card {
    // Name of card
    public String cardName;

    // Whether this card will wake up at night at all
    public boolean nightWakeup = false;

    // Ranking of when this card will wake up during night calls (the lower cards wake up first)
    public int ranking = 100;

    // The team this card is on
    public String team;

    // The server that the card is in, as well as the player
    public WerewolfServer server;

    // The ranking for winning a game
    public int winRank = 100;

    // Whether the card only wakes up during the first night or not
    public boolean firstNightOnly = false;

    // The rank to call prechecks
    public int preCheckRank = 1;

    // The rank to check deaths
    public int deathCheckRank = 5;

    // Whether the seer sees them as a werewolf or not
    public boolean isSeenAsWerewolf = false;

    // The method that returns information about the card when a player asks
    public abstract String help();

    // The method that will determine if this card won
    public abstract boolean won();

    // The method that is called whenever the first night happens. If it doesn't wake up during night, return. If it
    // doesn't have a special first night, call the other night method
    public abstract void firstNightWakeup();

    // The method that is called whenever any subsequent night happens. If it doesn't wake up during night, return
    public abstract void nightWakeup();

    // Some cards require things to be done after they are killed. Whenever they are killed, this method is called.
    public abstract void checkAfterDeaths();

    // The toSring method that prints the card name when called
    public String toString() { return cardName; }

    // The method to precheck required things for each card. This can be used to ensure there's only 1 of a specific
    // type of card
    public abstract void preCheck();
}
