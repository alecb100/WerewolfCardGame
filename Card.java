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
    public int winRank;

    // Whether the card only wakes up during the first night or not
    public boolean firstNightOnly = false;

    // Whether there's something to be done after all the dead
    public boolean deathCheck = false;

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

    public String toString() { return cardName; }
}
