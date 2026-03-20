import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GameStateTest {
    @Test
    public void returnHeldPileRestoresValuePileAndClearsHeldState() {
        GameState state = blankState();
        List<Card> held = new ArrayList<>(List.of(card(Suit.DIAMONDS, Rank.THREE), card(Suit.SPADES, Rank.A)));
        state.heldPile = held;
        state.heldRank = Rank.FIVE;
        state.lastDrawnCard = card(Suit.HEARTS, Rank.FIVE);

        state.returnHeldPile();

        assertSame(held, state.valuePiles.get(Rank.FIVE));
        assertNull(state.heldPile);
        assertNull(state.heldRank);
        assertNull(state.lastDrawnCard);
    }

    @Test
    public void drawReturnsFalseWhenDrawPileIsEmpty() {
        GameState state = blankState();

        assertFalse(state.draw());
    }

    @Test
    public void drawReturnsHeldPileThenPicksMatchingValuePile() {
        GameState state = blankState();

        List<Card> previousHeld = new ArrayList<>(List.of(card(Suit.CLUBS, Rank.A)));
        state.heldPile = previousHeld;
        state.heldRank = Rank.TWO;
        state.lastDrawnCard = card(Suit.DIAMONDS, Rank.TWO);

        List<Card> targetPile = state.valuePiles.get(Rank.K);
        Card existingTargetCard = card(Suit.SPADES, Rank.THREE);
        targetPile.add(existingTargetCard);

        Card drawn = card(Suit.HEARTS, Rank.K);
        state.drawPile.push(drawn);

        assertTrue(state.draw());
        assertSame(previousHeld, state.valuePiles.get(Rank.TWO));
        assertEquals(Rank.K, state.heldRank);
        assertEquals(drawn, state.lastDrawnCard);
        assertEquals(List.of(drawn, existingTargetCard), state.heldPile);
        assertEquals(0, state.valuePiles.get(Rank.K).size());
        assertTrue(state.drawPile.isEmpty());
    }

    @Test
    public void playFromValuePileMovesOnlyTopCardAndIncrementsScore() {
        GameState state = blankState();
        List<Card> aces = state.valuePiles.get(Rank.A);
        Card bottom = card(Suit.HEARTS, Rank.K);
        Card top = card(Suit.HEARTS, Rank.A);
        aces.add(bottom);
        aces.add(top);

        assertTrue(state.playFromValuePile(Rank.A, 2));
        assertEquals(1, state.score);
        assertEquals(List.of(bottom), aces);
        assertEquals(top, state.tableaus.get(2).top());
        assertFalse(state.playFromValuePile(Rank.A, 2));
        assertEquals(1, state.score);
    }

    @Test
    public void playFromValuePileReturnsFalseWhenSourcePileIsEmpty() {
        GameState state = blankState();

        assertFalse(state.playFromValuePile(Rank.Q, 0));
    }

    @Test
    public void playFromHeldIndexValidatesIndexAndPlacementRules() {
        GameState state = blankState();
        state.heldPile = new ArrayList<>(List.of(
                card(Suit.SPADES, Rank.THREE),
                card(Suit.CLUBS, Rank.K),
                card(Suit.CLUBS, Rank.A)));
        state.heldRank = Rank.A;

        assertFalse(state.playFromHeldIndex(-1, 0));
        assertFalse(state.playFromHeldIndex(9, 0));
        assertFalse(state.playFromHeldIndex(0, 0));

        assertTrue(state.playFromHeldIndex(2, 0));
        assertEquals(1, state.score);
        assertEquals(2, state.heldPile.size());
        assertEquals(card(Suit.CLUBS, Rank.A), state.tableaus.get(0).top());
    }

    @Test
    public void canPlayAnyCardChecksValuePileTopsAndHeldCards() {
        GameState state = blankState();
        assertFalse(state.canPlayAnyCard());

        state.valuePiles.get(Rank.A).add(card(Suit.SPADES, Rank.A));
        assertTrue(state.canPlayAnyCard());

        GameState topOnlyState = blankState();
        topOnlyState.valuePiles.get(Rank.A).add(card(Suit.HEARTS, Rank.A));
        topOnlyState.valuePiles.get(Rank.A).add(card(Suit.HEARTS, Rank.THREE));
        assertFalse(topOnlyState.canPlayAnyCard());

        GameState heldState = blankState();
        heldState.heldPile = new ArrayList<>(List.of(card(Suit.DIAMONDS, Rank.FOUR), card(Suit.DIAMONDS, Rank.A)));
        heldState.heldRank = Rank.A;
        assertTrue(heldState.canPlayAnyCard());
    }

    @Test
    public void isGameOverRequiresEmptyDrawPileAndNoLegalMoves() {
        GameState state = blankState();
        state.drawPile.push(card(Suit.CLUBS, Rank.TWO));
        assertFalse(state.isGameOver());

        state.drawPile.clear();
        state.valuePiles.get(Rank.A).add(card(Suit.HEARTS, Rank.A));
        assertFalse(state.isGameOver());

        state.valuePiles.get(Rank.A).clear();
        assertTrue(state.isGameOver());
    }

    private static GameState blankState() {
        GameState state = new GameState(0L);
        for (Rank rank : Rank.values()) {
            state.valuePiles.get(rank).clear();
        }
        for (TableauPile tableau : state.tableaus) {
            tableau.cards.clear();
        }
        state.drawPile.clear();
        state.heldPile = null;
        state.heldRank = null;
        state.lastDrawnCard = null;
        state.score = 0;
        return state;
    }

    private static Card card(Suit suit, Rank rank) {
        return new Card(suit, rank);
    }
}
