import java.util.ArrayList;
import java.util.List;

public class GameLogicTests {
    public static void main(String[] args) {
        testTableauCanPlace();
        testReturnHeldPileRestoresAndClears();
        testDrawReturnsFalseWhenEmpty();
        testDrawReturnsHeldThenPicksTargetPile();
        testPlayFromValuePileRequiresLegalTopCardAndScores();
        testPlayFromHeldIndexValidatesIndexAndScores();
        testCanPlayAnyCardChecksValuePileTopsAndHeldCards();
        testIsGameOverRequiresEmptyDrawPileAndNoMoves();
        System.out.println("All game logic tests passed.");
    }

    private static void testTableauCanPlace() {
        TableauPile up = new TableauPile(Suit.HEARTS, Direction.UP);
        assertTrue(up.canPlace(card(Suit.HEARTS, Rank.A)), "Empty up tableau should accept matching ace.");
        assertFalse(up.canPlace(card(Suit.HEARTS, Rank.TWO)), "Empty up tableau should reject non-ace.");
        assertFalse(up.canPlace(card(Suit.SPADES, Rank.A)), "Tableau should reject wrong suit.");

        up.place(card(Suit.HEARTS, Rank.A));
        assertTrue(up.canPlace(card(Suit.HEARTS, Rank.TWO)), "Up tableau should accept next rank.");
        assertFalse(up.canPlace(card(Suit.HEARTS, Rank.THREE)), "Up tableau should reject skipped rank.");

        TableauPile down = new TableauPile(Suit.CLUBS, Direction.DOWN);
        assertTrue(down.canPlace(card(Suit.CLUBS, Rank.K)), "Empty down tableau should accept matching king.");
        assertFalse(down.canPlace(card(Suit.CLUBS, Rank.Q)), "Empty down tableau should reject non-king.");

        down.place(card(Suit.CLUBS, Rank.K));
        assertTrue(down.canPlace(card(Suit.CLUBS, Rank.Q)), "Down tableau should accept previous rank.");
        assertFalse(down.canPlace(card(Suit.CLUBS, Rank.J)), "Down tableau should reject skipped rank.");
    }

    private static void testReturnHeldPileRestoresAndClears() {
        GameState state = blankState();
        List<Card> held = new ArrayList<>(List.of(card(Suit.DIAMONDS, Rank.THREE), card(Suit.SPADES, Rank.A)));
        state.heldPile = held;
        state.heldRank = Rank.FIVE;
        state.lastDrawnCard = card(Suit.HEARTS, Rank.FIVE);

        state.returnHeldPile();

        assertSame(held, state.valuePiles.get(Rank.FIVE), "Returned held pile should be restored to its value pile.");
        assertNull(state.heldPile, "Held pile should be cleared after return.");
        assertNull(state.heldRank, "Held rank should be cleared after return.");
        assertNull(state.lastDrawnCard, "Last drawn card should be cleared after return.");
    }

    private static void testDrawReturnsFalseWhenEmpty() {
        GameState state = blankState();
        assertFalse(state.draw(), "Draw should fail when the draw pile is empty.");
    }

    private static void testDrawReturnsHeldThenPicksTargetPile() {
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

        assertTrue(state.draw(), "Draw should succeed when a draw card exists.");
        assertSame(previousHeld, state.valuePiles.get(Rank.TWO), "Existing held pile should be returned before drawing.");
        assertEquals(Rank.K, state.heldRank, "Held rank should match the drawn card rank.");
        assertEquals(drawn, state.lastDrawnCard, "Last drawn card should track the drawn card.");
        assertEquals(List.of(drawn, existingTargetCard), state.heldPile,
                "Drawn card should be inserted at the bottom of the picked-up value pile.");
        assertEquals(0, state.valuePiles.get(Rank.K).size(), "Picked-up value pile should be replaced with an empty pile.");
        assertTrue(state.drawPile.isEmpty(), "Draw pile should lose the drawn card.");
    }

    private static void testPlayFromValuePileRequiresLegalTopCardAndScores() {
        GameState state = blankState();
        List<Card> aces = state.valuePiles.get(Rank.A);
        Card bottom = card(Suit.HEARTS, Rank.K);
        Card top = card(Suit.HEARTS, Rank.A);
        aces.add(bottom);
        aces.add(top);

        assertTrue(state.playFromValuePile(Rank.A, 2), "Playing a legal top card from a value pile should succeed.");
        assertEquals(1, state.score, "Successful value-pile play should increase score.");
        assertEquals(List.of(bottom), aces, "Only the top card should be removed from the source pile.");
        assertEquals(top, state.tableaus.get(2).top(), "Played card should land on the target tableau.");

        assertFalse(state.playFromValuePile(Rank.A, 2), "Illegal follow-up play should fail.");
        assertEquals(1, state.score, "Failed value-pile play should not change score.");

        GameState emptyState = blankState();
        assertFalse(emptyState.playFromValuePile(Rank.Q, 0), "Playing from an empty value pile should fail.");
    }

    private static void testPlayFromHeldIndexValidatesIndexAndScores() {
        GameState state = blankState();
        state.heldPile = new ArrayList<>(List.of(
                card(Suit.SPADES, Rank.THREE),
                card(Suit.CLUBS, Rank.K),
                card(Suit.CLUBS, Rank.A)));
        state.heldRank = Rank.A;

        assertFalse(state.playFromHeldIndex(-1, 0), "Negative held indexes should fail.");
        assertFalse(state.playFromHeldIndex(9, 0), "Out-of-range held indexes should fail.");
        assertFalse(state.playFromHeldIndex(0, 0), "Illegal tableau placement from held pile should fail.");

        assertTrue(state.playFromHeldIndex(2, 0), "Legal held-card play should succeed.");
        assertEquals(1, state.score, "Successful held-card play should increase score.");
        assertEquals(2, state.heldPile.size(), "Successful held-card play should remove exactly one held card.");
        assertEquals(card(Suit.CLUBS, Rank.A), state.tableaus.get(0).top(),
                "Played held card should land on the target tableau.");
    }

    private static void testCanPlayAnyCardChecksValuePileTopsAndHeldCards() {
        GameState state = blankState();
        assertFalse(state.canPlayAnyCard(), "State with no playable cards should report no legal move.");

        state.valuePiles.get(Rank.A).add(card(Suit.SPADES, Rank.A));
        assertTrue(state.canPlayAnyCard(), "Playable value-pile top card should be detected.");

        GameState topOnlyState = blankState();
        topOnlyState.valuePiles.get(Rank.A).add(card(Suit.HEARTS, Rank.A));
        topOnlyState.valuePiles.get(Rank.A).add(card(Suit.HEARTS, Rank.THREE));
        assertFalse(topOnlyState.canPlayAnyCard(), "Only the top card of a value pile should count for playability.");

        GameState heldState = blankState();
        heldState.heldPile = new ArrayList<>(List.of(card(Suit.DIAMONDS, Rank.FOUR), card(Suit.DIAMONDS, Rank.A)));
        heldState.heldRank = Rank.A;
        assertTrue(heldState.canPlayAnyCard(), "Any playable held card should count as a legal move.");
    }

    private static void testIsGameOverRequiresEmptyDrawPileAndNoMoves() {
        GameState state = blankState();
        state.drawPile.push(card(Suit.CLUBS, Rank.TWO));
        assertFalse(state.isGameOver(), "Game should not be over while draw pile still has cards.");

        state.drawPile.clear();
        state.valuePiles.get(Rank.A).add(card(Suit.HEARTS, Rank.A));
        assertFalse(state.isGameOver(), "Game should not be over while a legal play exists.");

        state.valuePiles.get(Rank.A).clear();
        assertTrue(state.isGameOver(), "Game should be over when draw pile is empty and no legal moves remain.");
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertNull(Object value, String message) {
        assertTrue(value == null, message + " Expected null but was: " + value);
    }

    private static void assertSame(Object expected, Object actual, String message) {
        assertTrue(expected == actual, message + " Expected same reference.");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        assertTrue(expected == null ? actual == null : expected.equals(actual),
                message + " Expected: " + expected + ", actual: " + actual);
    }
}
