import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
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
    private GameState state;
    private final JButton[] valuePileButtons = new JButton[Rank.values().length];
    private final JButton[] tableauButtons = new JButton[8];
    private final JPanel heldCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JLabel heldLabel = new JLabel("Held pile: none");
    private final JLabel drawLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Welcome to Two-Deck Solitaire");
    private final Map<String, ImageIcon> iconCache = new HashMap<>();
    private DragSelection dragPreviewSelection;
    private boolean gameOverDialogShown;

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
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> confirmAndStartNewGame());

        JPanel drawPanel = new JPanel(new BorderLayout(8, 8));
        drawPanel.setBorder(BorderFactory.createTitledBorder("Draw Pile"));
        JPanel drawControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        drawControlsPanel.add(drawButton);
        drawControlsPanel.add(drawLabel);
        drawPanel.add(drawControlsPanel, BorderLayout.CENTER);
        drawPanel.add(newGameButton, BorderLayout.SOUTH);
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

    private void confirmAndStartNewGame() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Start a new game? Your current progress will be lost.",
                "Confirm New Game",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            startNewGame();
        }
    }

    private void startNewGame() {
        state = new GameState(System.currentTimeMillis());
        dragPreviewSelection = null;
        gameOverDialogShown = false;
        statusLabel.setText("Started a new game.");
        refreshUi();
    }

    private void showGameOverDialogIfNeeded() {
        if (!state.isGameOver() || gameOverDialogShown) {
            return;
        }

        gameOverDialogShown = true;
        Object[] options = {"New Game", "Close"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Draw pile is empty and no legal moves remain.\nScore: " + state.score + "/" + GameState.TOTAL_CARDS,
                "Game Over",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            startNewGame();
        }
    }

    private void installDragSource(JButton button) {
        DragSourceTransferHandler transferHandler = new DragSourceTransferHandler();
        button.setTransferHandler(transferHandler);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JComponent source = (JComponent) e.getSource();
                Object payload = source.getClientProperty("dragSelection");
                if (payload != null) {
                    DragSelection selection = DragSelection.decode(payload.toString());
                    transferHandler.configureDragImage(button, e.getPoint());
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

        String[] resourceCandidates = {
            "/cards/" + fileName,
            "/" + fileName
        };

        for (String resourceCandidate : resourceCandidates) {
            URL resource = getClass().getResource(resourceCandidate);
            if (resource != null) {
                ImageIcon raw = new ImageIcon(resource);
                Image scaled = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);
                iconCache.put(cacheKey, icon);
                return icon;
            }
        }

        String[] fileCandidates = {
            "cards/" + fileName,
            fileName
        };

        for (String fileCandidate : fileCandidates) {
            ImageIcon raw = new ImageIcon(fileCandidate);
            if (raw.getIconWidth() > 0 && raw.getIconHeight() > 0) {
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
        void configureDragImage(JButton button, Point dragOrigin) {
            Icon icon = button.getIcon();
            if (icon instanceof ImageIcon imageIcon) {
                setDragImage(imageIcon.getImage());
                setDragImageOffset(new Point(
                        Math.min(dragOrigin.x, imageIcon.getIconWidth() - 1),
                        Math.min(dragOrigin.y, imageIcon.getIconHeight() - 1)));
                return;
            }

            setDragImage(null);
            setDragImageOffset(new Point());
        }

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
