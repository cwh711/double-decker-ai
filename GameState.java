import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameState {
    static final int TOTAL_CARDS = 104;

    final Map<Rank, List<Card>> valuePiles = new EnumMap<>(Rank.class);
    final List<TableauPile> tableaus = new ArrayList<>();
    final Deque<Card> drawPile = new ArrayDeque<>();
    List<Card> heldPile;
    Rank heldRank;
    Card lastDrawnCard;
    int score;

    GameState(long seed) {
        for (Rank rank : Rank.values()) {
            valuePiles.put(rank, new ArrayList<>());
        }
        for (Suit suit : Suit.values()) {
            tableaus.add(new TableauPile(suit, Direction.UP));
        }
        for (Suit suit : Suit.values()) {
            tableaus.add(new TableauPile(suit, Direction.DOWN));
        }
        deal(seed);
    }

    private void deal(long seed) {
        List<Card> deck = new ArrayList<>(TOTAL_CARDS);
        for (int copies = 0; copies < 2; copies++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    deck.add(new Card(suit, rank));
                }
            }
        }

        Collections.shuffle(deck, new Random(seed));
        int nextLabelIndex = 0;

        while (!deck.isEmpty()) {
            Rank label = Rank.values()[nextLabelIndex];
            Card dealt = deck.remove(deck.size() - 1);
            valuePiles.get(label).add(dealt);

            int extraToDraw = 0;
            if (dealt.rank() == label) {
                extraToDraw++;
            }
            if (label == Rank.TEN || label == Rank.K) {
                extraToDraw++;
            }
            if (dealt.rank() == Rank.A) {
                extraToDraw += 2;
            }

            for (int i = 0; i < extraToDraw && !deck.isEmpty(); i++) {
                drawPile.push(deck.remove(deck.size() - 1));
            }

            nextLabelIndex = (nextLabelIndex + 1) % Rank.values().length;
        }
    }

    void returnHeldPile() {
        if (heldPile != null && heldRank != null) {
            valuePiles.put(heldRank, heldPile);
            heldPile = null;
            heldRank = null;
            lastDrawnCard = null;
        }
    }

    boolean draw() {
        if (drawPile.isEmpty()) {
            return false;
        }

        returnHeldPile();
        Card drawn = drawPile.pop();
        Rank targetRank = drawn.rank();
        List<Card> picked = valuePiles.get(targetRank);
        valuePiles.put(targetRank, new ArrayList<>());
        picked.add(0, drawn);
        heldPile = picked;
        heldRank = targetRank;
        lastDrawnCard = drawn;
        return true;
    }

    boolean playFromValuePile(Rank rank, int tableauIndex) {
        List<Card> pile = valuePiles.get(rank);
        if (pile.isEmpty()) {
            return false;
        }
        Card card = pile.get(pile.size() - 1);
        TableauPile tableau = tableaus.get(tableauIndex);
        if (!tableau.canPlace(card)) {
            return false;
        }
        pile.remove(pile.size() - 1);
        tableau.place(card);
        score++;
        return true;
    }

    boolean playFromHeldIndex(int cardIndex, int tableauIndex) {
        if (heldPile == null || cardIndex < 0 || cardIndex >= heldPile.size()) {
            return false;
        }
        Card card = heldPile.get(cardIndex);
        TableauPile tableau = tableaus.get(tableauIndex);
        if (!tableau.canPlace(card)) {
            return false;
        }
        heldPile.remove(cardIndex);
        tableau.place(card);
        score++;
        return true;
    }

    boolean canPlayAnyCard() {
        for (Rank rank : Rank.values()) {
            List<Card> pile = valuePiles.get(rank);
            if (!pile.isEmpty() && canPlaceOnAnyTableau(pile.get(pile.size() - 1))) {
                return true;
            }
        }
        if (heldPile != null) {
            for (Card card : heldPile) {
                if (canPlaceOnAnyTableau(card)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPlaceOnAnyTableau(Card card) {
        for (TableauPile tableau : tableaus) {
            if (tableau.canPlace(card)) {
                return true;
            }
        }
        return false;
    }

    boolean isGameOver() {
        return drawPile.isEmpty() && !canPlayAnyCard();
    }
}
