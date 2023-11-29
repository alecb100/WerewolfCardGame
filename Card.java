public abstract class Card {
    // Name of card
    public String cardName;

    // Whether this card will wake up at night at all
    public boolean nightWakeup;

    // Ranking of when this card will wake up during night calls (the lower cards wake up first)
    public int ranking;

    // The player that has this role
    public Player player;

    // The team this card is on
    public String team;

    // The server that the card is in, as well as the player
    public WerewolfServer server;

    // The method that will determine if this card won
    public abstract boolean won();

    // The method that is called whenever night happens. If it doesn't wake up during night, return
    public abstract void nightWakeup();
}
