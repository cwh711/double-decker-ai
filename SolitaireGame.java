import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
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
            return suit.fileName + "_" + rank.fileName + ".png";
        }

        @Override
        public String toString() {
            return rank.label + suit.symbol;
        }
    }

    record DragSelection(Rank valueRank, Integer heldIndex) {
        String encode() {
            if (valueRank != null) {
                return "VALUE:" + valueRank.name();
            }
            return "HELD:" + heldIndex;
        }

        static DragSelection decode(String encoded) {
            if (encoded.startsWith("VALUE:")) {
                return new DragSelection(Rank.valueOf(encoded.substring("VALUE:".length())), null);
            }
            if (encoded.startsWith("HELD:")) {
                return new DragSelection(null, Integer.parseInt(encoded.substring("HELD:".length())));
            }
            throw new IllegalArgumentException("Unknown drag payload: " + encoded);
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

    private final GameState state;
    private final JButton[] valuePileButtons = new JButton[Rank.values().length];
    private final JButton[] tableauButtons = new JButton[8];
    private final JPanel heldCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JLabel heldLabel = new JLabel("Held pile: none");
    private final JLabel drawLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Welcome to Two-Deck Solitaire");
    private final Map<String, ImageIcon> iconCache = new HashMap<>();
    private DragSelection dragPreviewSelection;

    public SolitaireGame(long seed) {
        super("Two-Deck Solitaire (GUI)");
        this.state = new GameState(seed);
        configureUi();
        refreshUi();
    }

    private void configureUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        add(buildTableauPanel(), BorderLayout.NORTH);
        add(buildValuePilesPanel(), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        JButton drawButton = new JButton("Draw");
        drawButton.addActionListener(e -> onDraw());

        JPanel drawPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        drawPanel.setBorder(BorderFactory.createTitledBorder("Draw Pile"));
        drawPanel.add(drawButton);
        drawPanel.add(drawLabel);
        bottomPanel.add(drawPanel, BorderLayout.WEST);

        JPanel heldPanel = new JPanel(new BorderLayout(8, 8));
        heldPanel.setBorder(BorderFactory.createTitledBorder("Picked-up Value Pile"));
        heldPanel.add(heldLabel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(heldCardsPanel);
        scrollPane.setPreferredSize(new Dimension(680, 120));
        heldPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(heldPanel, BorderLayout.CENTER);

        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(245, 245, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(980, 700);
        setLocationRelativeTo(null);
    }

    private JPanel buildValuePilesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 13, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Value Piles (drag top card onto a tableau)"));

        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            Rank rank = ranks[i];
            JButton button = new JButton(rank.label);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setFont(button.getFont().deriveFont(Font.PLAIN, 11f));
            installDragSource(button);
            valuePileButtons[i] = button;
            panel.add(button);
        }

        return panel;
    }

    private JPanel buildTableauPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Tableaus (drop cards here)"));

        for (int i = 0; i < 8; i++) {
            JButton button = new JButton();
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setTransferHandler(new TableauTransferHandler(i));
            tableauButtons[i] = button;
            panel.add(button);
        }

        return panel;
    }

    private void onDraw() {
        boolean drew = state.draw();

        if (!drew) {
            statusLabel.setText("Draw pile is empty.");
        } else {
            statusLabel.setText("Drew " + state.lastDrawnCard + " and picked up " + state.heldRank.label + " pile.");
        }

        refreshUi();
        showGameOverDialogIfNeeded();
    }

    private void onDropToTableau(DragSelection selection, int tableauIndex) {
        boolean success = false;

        if (selection.valueRank() != null) {
            success = state.playFromValuePile(selection.valueRank(), tableauIndex);
        } else if (selection.heldIndex() != null) {
            success = state.playFromHeldIndex(selection.heldIndex(), tableauIndex);
        }

        if (success) {
            statusLabel.setText("Card placed on tableau " + (tableauIndex + 1) + ".");
        } else {
            statusLabel.setText("Illegal move.");
        }

        refreshUi();
        showGameOverDialogIfNeeded();
    }

    private void showGameOverDialogIfNeeded() {
        if (!state.isGameOver()) {
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Draw pile is empty and no legal moves remain.\nScore: " + state.score,
                "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void installDragSource(JButton button) {
        button.setTransferHandler(new DragSourceTransferHandler());
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JComponent source = (JComponent) e.getSource();
                Object payload = source.getClientProperty("dragSelection");
                if (payload != null) {
                    DragSelection selection = DragSelection.decode(payload.toString());
                    if (selection.valueRank() != null) {
                        dragPreviewSelection = selection;
                        refreshUi();
                    }
                    source.getTransferHandler().exportAsDrag(source, e, TransferHandler.COPY);
                }
            }
        });
    }

    private void refreshUi() {
        drawLabel.setText("Cards left: " + state.drawPile.size());

        for (int i = 0; i < Rank.values().length; i++) {
            Rank rank = Rank.values()[i];
            JButton button = valuePileButtons[i];
            List<Card> pile = state.valuePiles.get(rank);
            Card top = cardShownForValuePile(rank, pile);
            updateCardButton(button, top, rank.label + "\n(" + pile.size() + ")");
            button.putClientProperty("dragSelection", pile.isEmpty() ? null : new DragSelection(rank, null).encode());
            button.setBorder(UIManager.getBorder("Button.border"));
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
                cardButton.putClientProperty("dragSelection", new DragSelection(null, index).encode());
                installDragSource(cardButton);
                heldCardsPanel.add(cardButton);
            }
        }

        heldCardsPanel.revalidate();
        heldCardsPanel.repaint();
    }

    private Card cardShownForValuePile(Rank rank, List<Card> pile) {
        if (pile.isEmpty()) {
            return null;
        }
        if (dragPreviewSelection != null && dragPreviewSelection.valueRank() == rank) {
            return pile.size() > 1 ? pile.get(pile.size() - 2) : null;
        }
        return pile.get(pile.size() - 1);
    }

    private void updateCardButton(JButton button, Card card, String fallbackText) {
        button.setPreferredSize(new Dimension(90, 120));
        if (card == null) {
            ImageIcon emptyIcon = loadEmptyTableauIconFromFallback(fallbackText, 74, 98);
            button.setIcon(emptyIcon);
            button.setText("<html>" + fallbackText.replace("\n", "<br>") + "</html>");
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
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

    private ImageIcon loadEmptyTableauIconFromFallback(String fallbackText, int width, int height) {
        for (Suit suit : Suit.values()) {
            if (fallbackText.contains(suit.name())) {
                return loadImageIcon(suit.fileName + "_empty.png", width, height);
            }
        }
        return null;
    }

    private ImageIcon loadCardIcon(Card card, int width, int height) {
        return loadImageIcon(card.imageFileName(), width, height);
    }

    private ImageIcon loadImageIcon(String fileName, int width, int height) {
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

    private class DragSourceTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(JComponent c) {
            Object payload = c.getClientProperty("dragSelection");
            if (payload == null) {
                return null;
            }
            return new StringSelection(payload.toString());
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            dragPreviewSelection = null;
            refreshUi();
        }
    }

    private class TableauTransferHandler extends TransferHandler {
        private final int tableauIndex;

        TableauTransferHandler(int tableauIndex) {
            this.tableauIndex = tableauIndex;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                String payload = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                onDropToTableau(DragSelection.decode(payload), tableauIndex);
                return true;
            } catch (Exception e) {
                statusLabel.setText("Could not read dragged card.");
                return false;
            }
        }
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
