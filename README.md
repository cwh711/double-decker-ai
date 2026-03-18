# double-decker-ai

GUI implementation of the two-deck solitaire variant in Java Swing.

## Build and run

```bash
javac SolitaireGame.java
java SolitaireGame
```

Optionally pass a numeric seed for deterministic shuffles:

```bash
java SolitaireGame 42
```

## How to play in the GUI

- Click **Draw** to draw from the face-down draw pile.
- Drawing automatically returns any currently held pile, then picks up the pile matching the drawn card value and places the drawn card on the bottom.
- To play a card to a tableau:
  - Click a **value pile** to select its top card, or
  - Click a specific card in the **held pile** panel,
  - Then click the target **tableau**.

Tableau order:
- `1-4`: build up (`A→K`) for Clubs, Diamonds, Hearts, Spades
- `5-8`: build down (`K→A`) for Clubs, Diamonds, Hearts, Spades

## Card images

The UI supports card images named as:

```text
[suit]-[value].png
```

Examples: `hearts-a.png`, `clubs-10.png`, `spades-k.png`.

Images are loaded from either:
- `cards/` directory (preferred), or
- project root.

If an image is missing, the UI falls back to text labels.
