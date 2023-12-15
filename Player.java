import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Player {
    public Card card;

    public String name;

    public ObjectInputStream input;

    public ObjectOutputStream output;

    public boolean dead;

    public boolean tower;

    public boolean canVote;

    public Player(String name, ObjectInputStream input, ObjectOutputStream output) {
        this.name = name;
        this.input = input;
        this.output = output;
        dead = false;
        tower = false;
        canVote = true;
    }

    @Override
    public String toString() {
        return name;
    }
}
