import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SolitaireGame extends JFrame {
    enum Suit {
        CLUBS("clubs", "♣"), DIAMONDS("diamonds", "♦"), HEARTS("hearts", "♥"), SPADES("spades", "♠");

        final String fileName;
        final String symbol;

        Suit(String fileName, String symbol) {
            this.fileName = fileName;
            this.symbol = symbol;
        }
    }

    enum Rank {
        A(1, "a", "A"),
        TWO(2, "2", "2"),
        THREE(3, "3", "3"),
        FOUR(4, "4", "4"),
        FIVE(5, "5", "5"),
        SIX(6, "6", "6"),
        SEVEN(7, "7", "7"),
        EIGHT(8, "8", "8"),
        NINE(9, "9", "9"),
        TEN(10, "10", "10"),
        J(11, "j", "J"),
        Q(12, "q", "Q"),
        K(13, "k", "K");

        final int value;
        final String fileName;
        final String label;

        Rank(int value, String fileName, String label) {
            this.value = value;
            this.fileName = fileName;
            this.label = label;
        }
    }

    enum Direction { UP, DOWN }

    record Card(Suit suit, Rank rank) {
        String imageFileName() {
            return suit.fileName + "-" + rank.fileName + ".png";
        }

        @Override
        public String toString() {
            return rank.label + suit.symbol;
        }
    }

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

        Card top() {
            return cards.isEmpty() ? null : cards.get(cards.size() - 1);
        }

        String label() {
            return suit.name() + " " + (direction == Direction.UP ? "A→K" : "K→A");
        }
    }

    static class GameState {
        final Map<Rank, List<Card>> valuePiles = new EnumMap<>(Rank.class);
        final List<TableauPile> tableaus = new ArrayList<>();
        final Deque<Card> drawPile = new ArrayDeque<>();
        List<Card> heldPile;
        Rank heldRank;
        Card lastDrawnCard;

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
            Rank targetRank = drawn.rank;
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

    private final GameState state;
    private final JButton[] valuePileButtons = new JButton[Rank.values().length];
    private final JButton[] tableauButtons = new JButton[8];
    private final JPanel heldCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JLabel heldLabel = new JLabel("Held pile: none");
    private final JLabel drawLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Welcome to Two-Deck Solitaire");
    private final Map<String, ImageIcon> iconCache = new HashMap<>();

    private Rank selectedValueRank;
    private Integer selectedHeldIndex;

    public SolitaireGame(long seed) {
        super("Two-Deck Solitaire (GUI)");
        this.state = new GameState(seed);
        configureUi();
        refreshUi();
    }

    private void configureUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JButton drawButton = new JButton("Draw");
        drawButton.addActionListener(e -> onDraw());

        JPanel drawPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        drawPanel.setBorder(BorderFactory.createTitledBorder("Draw Pile"));
        drawPanel.add(drawButton);
        drawPanel.add(drawLabel);
        topPanel.add(drawPanel, BorderLayout.WEST);

        JPanel heldPanel = new JPanel(new BorderLayout(8, 8));
        heldPanel.setBorder(BorderFactory.createTitledBorder("Picked-up Value Pile"));
        heldPanel.add(heldLabel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(heldCardsPanel);
        scrollPane.setPreferredSize(new Dimension(680, 120));
        heldPanel.add(scrollPane, BorderLayout.CENTER);
        topPanel.add(heldPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        centerPanel.add(buildValuePilesPanel());
        centerPanel.add(buildTableauPanel());
        add(centerPanel, BorderLayout.CENTER);

        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(245, 245, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(statusLabel, BorderLayout.SOUTH);

        setSize(980, 700);
        setLocationRelativeTo(null);
    }

    private JPanel buildValuePilesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 13, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Value Piles (click to play top card)"));

        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            Rank rank = ranks[i];
            JButton button = new JButton(rank.label);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setFont(button.getFont().deriveFont(Font.PLAIN, 11f));
            button.addActionListener(e -> onSelectValuePile(rank));
            valuePileButtons[i] = button;
            panel.add(button);
        }

        return panel;
    }

    private JPanel buildTableauPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Tableaus (click destination after selecting source card)"));

        for (int i = 0; i < 8; i++) {
            JButton button = new JButton();
            int tableauIndex = i;
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.addActionListener(e -> onPlayToTableau(tableauIndex));
            tableauButtons[i] = button;
            panel.add(button);
        }

        return panel;
    }

    private void onDraw() {
        boolean drew = state.draw();
        clearSelection();

        if (!drew) {
            statusLabel.setText("Draw pile is empty.");
        } else {
            statusLabel.setText("Drew " + state.lastDrawnCard + " and picked up " + state.heldRank.label + " pile.");
        }

        refreshUi();
        if (state.isGameOver()) {
            JOptionPane.showMessageDialog(this, "Draw pile is empty and no legal moves remain.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onSelectValuePile(Rank rank) {
        selectedValueRank = rank;
        selectedHeldIndex = null;
        statusLabel.setText("Selected top card from " + rank.label + " pile. Click a tableau to play.");
        refreshUi();
    }

    private void onSelectHeldCard(int heldIndex, Card card) {
        selectedHeldIndex = heldIndex;
        selectedValueRank = null;
        statusLabel.setText("Selected held card " + card + ". Click a tableau to play.");
        refreshUi();
    }

    private void onPlayToTableau(int tableauIndex) {
        boolean success = false;

        if (selectedValueRank != null) {
            success = state.playFromValuePile(selectedValueRank, tableauIndex);
        } else if (selectedHeldIndex != null) {
            success = state.playFromHeldIndex(selectedHeldIndex, tableauIndex);
        }

        if (success) {
            statusLabel.setText("Card placed on tableau " + (tableauIndex + 1) + ".");
            clearSelection();
        } else {
            statusLabel.setText("Illegal move or no source card selected.");
        }

        refreshUi();
        if (state.isGameOver()) {
            JOptionPane.showMessageDialog(this, "Draw pile is empty and no legal moves remain.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearSelection() {
        selectedValueRank = null;
        selectedHeldIndex = null;
    }

    private void refreshUi() {
        drawLabel.setText("Cards left: " + state.drawPile.size());

        for (int i = 0; i < Rank.values().length; i++) {
            Rank rank = Rank.values()[i];
            JButton button = valuePileButtons[i];
            List<Card> pile = state.valuePiles.get(rank);
            Card top = pile.isEmpty() ? null : pile.get(pile.size() - 1);
            updateCardButton(button, top, rank.label + "\n(" + pile.size() + ")");

            if (selectedValueRank == rank) {
                button.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                button.setBorder(UIManager.getBorder("Button.border"));
            }
        }

        for (int i = 0; i < state.tableaus.size(); i++) {
            TableauPile tableau = state.tableaus.get(i);
            JButton button = tableauButtons[i];
            Card top = tableau.top();
            String fallback = "[" + (i + 1) + "] " + tableau.label() + "\n(" + tableau.cards.size() + ")";
            updateCardButton(button, top, fallback);
        }

        heldCardsPanel.removeAll();
        if (state.heldPile == null) {
            heldLabel.setText("Held pile: none");
        } else {
            heldLabel.setText("Held pile " + state.heldRank.label + " (top on left, size " + state.heldPile.size() + ")");
            for (int i = state.heldPile.size() - 1; i >= 0; i--) {
                Card card = state.heldPile.get(i);
                int index = i;
                JButton cardButton = new JButton(card.toString());
                cardButton.setPreferredSize(new Dimension(90, 110));
                ImageIcon icon = loadCardIcon(card, 72, 96);
                if (icon != null) {
                    cardButton.setIcon(icon);
                    cardButton.setText("#" + (state.heldPile.size() - i));
                    cardButton.setVerticalTextPosition(SwingConstants.BOTTOM);
                    cardButton.setHorizontalTextPosition(SwingConstants.CENTER);
                }
                cardButton.addActionListener(e -> onSelectHeldCard(index, card));

                if (selectedHeldIndex != null && selectedHeldIndex == index) {
                    cardButton.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                }
                heldCardsPanel.add(cardButton);
            }
        }

        heldCardsPanel.revalidate();
        heldCardsPanel.repaint();
    }

    private void updateCardButton(JButton button, Card card, String fallbackText) {
        button.setPreferredSize(new Dimension(90, 120));
        if (card == null) {
            button.setIcon(null);
            button.setText("<html>" + fallbackText.replace("\n", "<br>") + "</html>");
            return;
        }

        ImageIcon icon = loadCardIcon(card, 74, 98);
        if (icon != null) {
            button.setIcon(icon);
            button.setText("<html>" + fallbackText.replace("\n", "<br>") + "</html>");
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
        } else {
            button.setIcon(null);
            button.setText("<html>" + card + "<br>" + fallbackText.replace("\n", "<br>") + "</html>");
        }
    }

    private ImageIcon loadCardIcon(Card card, int width, int height) {
        String fileName = card.imageFileName();
        String cacheKey = fileName + "@" + width + "x" + height;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        Path[] candidates = {
            Path.of("cards", fileName),
            Path.of(fileName)
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                ImageIcon raw = new ImageIcon(candidate.toString());
                Image scaled = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);
                iconCache.put(cacheKey, icon);
                return icon;
            }
        }

        iconCache.put(cacheKey, null);
        return null;
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

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Cannot launch GUI in a headless environment.");
            return;
        }

        long finalSeed = seed;
        SwingUtilities.invokeLater(() -> new SolitaireGame(finalSeed).setVisible(true));
    }
}
