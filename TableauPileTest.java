import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableauPileTest {
    @Test
    public void canPlaceHonorsSuitAndAscendingRules() {
        TableauPile up = new TableauPile(Suit.HEARTS, Direction.UP);

        assertTrue(up.canPlace(card(Suit.HEARTS, Rank.A)));
        assertFalse(up.canPlace(card(Suit.HEARTS, Rank.TWO)));
        assertFalse(up.canPlace(card(Suit.SPADES, Rank.A)));

        up.place(card(Suit.HEARTS, Rank.A));

        assertTrue(up.canPlace(card(Suit.HEARTS, Rank.TWO)));
        assertFalse(up.canPlace(card(Suit.HEARTS, Rank.THREE)));
    }

    @Test
    public void canPlaceHonorsDescendingRules() {
        TableauPile down = new TableauPile(Suit.CLUBS, Direction.DOWN);

        assertTrue(down.canPlace(card(Suit.CLUBS, Rank.K)));
        assertFalse(down.canPlace(card(Suit.CLUBS, Rank.Q)));
        assertFalse(down.canPlace(card(Suit.HEARTS, Rank.K)));

        down.place(card(Suit.CLUBS, Rank.K));

        assertTrue(down.canPlace(card(Suit.CLUBS, Rank.Q)));
        assertFalse(down.canPlace(card(Suit.CLUBS, Rank.J)));
    }

    private static Card card(Suit suit, Rank rank) {
        return new Card(suit, rank);
    }
}
