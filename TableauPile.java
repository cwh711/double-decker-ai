import java.util.ArrayList;
import java.util.List;

public class TableauPile {
    final Suit suit;
    final Direction direction;
    final List<Card> cards = new ArrayList<>();

    TableauPile(Suit suit, Direction direction) {
        this.suit = suit;
        this.direction = direction;
    }

    boolean canPlace(Card card) {
        if (card.suit() != suit) {
            return false;
        }
        if (cards.isEmpty()) {
            return (direction == Direction.UP && card.rank() == Rank.A)
                    || (direction == Direction.DOWN && card.rank() == Rank.K);
        }

        Card top = cards.get(cards.size() - 1);
        int expected = direction == Direction.UP ? top.rank().value + 1 : top.rank().value - 1;
        return card.rank().value == expected;
    }

    void place(Card card) {
        cards.add(card);
    }

    Card top() {
        return cards.isEmpty() ? null : cards.get(cards.size() - 1);
    }

    String label() {
        return suit.name() + " " + (direction == Direction.UP ? "A→K" : "K→A");
    }
}
