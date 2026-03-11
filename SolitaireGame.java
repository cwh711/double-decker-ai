import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class SolitaireGame {
    enum Suit {
        CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠");

        final String symbol;

        Suit(String symbol) {
            this.symbol = symbol;
        }
    }

    enum Rank {
        A(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10), J(11), Q(12), K(13);

        final int value;

        Rank(int value) {
            this.value = value;
        }

        static Rank fromLabel(String label) {
            String normalized = label.trim().toUpperCase();
            return switch (normalized) {
                case "A", "1" -> A;
                case "2" -> TWO;
                case "3" -> THREE;
                case "4" -> FOUR;
                case "5" -> FIVE;
                case "6" -> SIX;
                case "7" -> SEVEN;
                case "8" -> EIGHT;
                case "9" -> NINE;
                case "10", "T" -> TEN;
                case "J", "11" -> J;
                case "Q", "12" -> Q;
                case "K", "13" -> K;
                default -> null;
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case A -> "A";
                case TWO -> "2";
                case THREE -> "3";
                case FOUR -> "4";
                case FIVE -> "5";
                case SIX -> "6";
                case SEVEN -> "7";
                case EIGHT -> "8";
                case NINE -> "9";
                case TEN -> "10";
                case J -> "J";
                case Q -> "Q";
                case K -> "K";
            };
        }
    }

    record Card(Suit suit, Rank rank) {
        @Override
        public String toString() {
            return rank + suit.symbol;
        }
    }

    enum Direction { UP, DOWN }

    static class TableauPile {
        final Suit suit;
        final Direction direction;
        final List<Card> cards = new ArrayList<>();

        TableauPile(Suit suit, Direction direction) {
            this.suit = suit;
            this.direction = direction;
        }

        boolean canPlace(Card card) {
            if (card.suit != suit) {
                return false;
            }
            if (cards.isEmpty()) {
                return (direction == Direction.UP && card.rank == Rank.A)
                        || (direction == Direction.DOWN && card.rank == Rank.K);
            }
            Card top = cards.get(cards.size() - 1);
            int expected = direction == Direction.UP ? top.rank.value + 1 : top.rank.value - 1;
            return card.rank.value == expected;
        }

        void place(Card card) {
            cards.add(card);
        }

        String label() {
            return suit + "-" + direction;
        }

        String topString() {
            return cards.isEmpty() ? "(empty)" : cards.get(cards.size() - 1).toString();
        }
    }

    private final Map<Rank, List<Card>> valuePiles = new EnumMap<>(Rank.class);
    private final List<TableauPile> tableaus = new ArrayList<>();
    private final Deque<Card> drawPile = new ArrayDeque<>();

    private List<Card> heldPile;
    private Rank heldRank;
    private Card lastDrawnCard;

    public SolitaireGame(long seed) {
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
        List<Card> deck = new ArrayList<>(104);
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
            if (dealt.rank == label) {
                extraToDraw++;
            }
            if (label == Rank.TEN || label == Rank.K) {
                extraToDraw++;
            }
            if (dealt.rank == Rank.A) {
                extraToDraw += 2;
            }

            for (int i = 0; i < extraToDraw && !deck.isEmpty(); i++) {
                drawPile.push(deck.remove(deck.size() - 1));
            }

            nextLabelIndex = (nextLabelIndex + 1) % Rank.values().length;
        }
    }

    private void returnHeldPile() {
        if (heldPile != null && heldRank != null) {
            valuePiles.put(heldRank, heldPile);
            heldPile = null;
            heldRank = null;
            lastDrawnCard = null;
        }
    }

    private boolean draw() {
        if (drawPile.isEmpty()) {
            return false;
        }

        returnHeldPile();

        Card drawn = drawPile.pop();
        Rank targetRank = drawn.rank;
        List<Card> picked = valuePiles.get(targetRank);
        valuePiles.put(targetRank, new ArrayList<>());
        picked.add(0, drawn); // Drawn card is placed on the bottom.

        heldPile = picked;
        heldRank = targetRank;
        lastDrawnCard = drawn;
        return true;
    }

    private boolean playFromValuePile(Rank rank, int tableauIndex) {
        if (tableauIndex < 0 || tableauIndex >= tableaus.size()) {
            return false;
        }
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
        return true;
    }

    private boolean playFromHeld(int positionFromTop, int tableauIndex) {
        if (heldPile == null || tableauIndex < 0 || tableauIndex >= tableaus.size()) {
            return false;
        }
        if (positionFromTop < 1 || positionFromTop > heldPile.size()) {
            return false;
        }
        int index = heldPile.size() - positionFromTop;
        Card card = heldPile.get(index);
        TableauPile tableau = tableaus.get(tableauIndex);
        if (!tableau.canPlace(card)) {
            return false;
        }
        heldPile.remove(index);
        tableau.place(card);
        return true;
    }

    private boolean canPlayAnyCard() {
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

    private boolean isGameOver() {
        return drawPile.isEmpty() && !canPlayAnyCard();
    }

    private void printState() {
        System.out.println("\n=== Game State ===");
        System.out.println("Draw pile size: " + drawPile.size());
        if (lastDrawnCard != null) {
            System.out.println("Held pile: " + heldRank + " (drawn " + lastDrawnCard + ")");
            printHeldPreview();
        } else {
            System.out.println("Held pile: none");
        }

        System.out.println("\nValue piles (top card shown):");
        for (Rank rank : Rank.values()) {
            List<Card> pile = valuePiles.get(rank);
            String top = pile.isEmpty() ? "(empty)" : pile.get(pile.size() - 1).toString();
            System.out.printf("  %-2s: %-8s size=%d%n", rank, top, pile.size());
        }

        System.out.println("\nTableaus:");
        for (int i = 0; i < tableaus.size(); i++) {
            TableauPile t = tableaus.get(i);
            System.out.printf("  [%d] %-12s top=%s size=%d%n", i + 1, t.label(), t.topString(), t.cards.size());
        }
        System.out.println();
    }

    private void printHeldPreview() {
        if (heldPile == null || heldPile.isEmpty()) {
            System.out.println("  Held cards: (empty)");
            return;
        }
        System.out.print("  Held cards top->bottom: ");
        for (int i = heldPile.size() - 1; i >= 0; i--) {
            System.out.print(heldPile.get(i));
            if (i > 0) {
                System.out.print(" ");
            }
        }
        System.out.println();
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  draw");
        System.out.println("  play pile <rank> <tableau#>   e.g. play pile Q 3");
        System.out.println("  play held <posFromTop> <tableau#>  e.g. play held 1 6");
        System.out.println("  show");
        System.out.println("  help");
        System.out.println("  quit");
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Two-Deck Solitaire started.");
        printHelp();
        printState();

        while (true) {
            if (isGameOver()) {
                System.out.println("No more legal plays and draw pile is empty. Game over.");
                break;
            }

            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "draw" -> {
                    if (draw()) {
                        System.out.println("Drew and picked up pile " + heldRank + ".");
                    } else {
                        System.out.println("Draw pile is empty.");
                    }
                    printState();
                }
                case "play" -> {
                    boolean success = false;
                    if (parts.length == 4 && parts[1].equalsIgnoreCase("pile")) {
                        Rank rank = Rank.fromLabel(parts[2]);
                        int tableauIndex = parseTableauIndex(parts[3]);
                        if (rank != null && tableauIndex >= 0) {
                            success = playFromValuePile(rank, tableauIndex);
                        }
                    } else if (parts.length == 4 && parts[1].equalsIgnoreCase("held")) {
                        Integer pos = parsePositiveInt(parts[2]);
                        int tableauIndex = parseTableauIndex(parts[3]);
                        if (pos != null && tableauIndex >= 0) {
                            success = playFromHeld(pos, tableauIndex);
                        }
                    }

                    System.out.println(success ? "Play succeeded." : "Illegal play or invalid command format.");
                    printState();
                }
                case "show" -> printState();
                case "help" -> printHelp();
                case "quit", "exit" -> {
                    System.out.println("Exiting game.");
                    return;
                }
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private static Integer parsePositiveInt(String value) {
        try {
            int i = Integer.parseInt(value);
            return i > 0 ? i : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseTableauIndex(String value) {
        Integer number = parsePositiveInt(value);
        if (number == null || number > 8) {
            return -1;
        }
        return number - 1;
    }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        if (args.length == 1) {
            try {
                seed = Long.parseLong(args[0]);
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid seed provided; using current time.");
            }
        }
        new SolitaireGame(seed).run();
    }
}
