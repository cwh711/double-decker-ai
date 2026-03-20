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

You can also build and run tests with Maven:

```bash
mvn package
java -jar target/double-decker-ai-1.0-SNAPSHOT.jar
mvn test
```

## How to play in the GUI

- The **tableaus** are shown at the top of the window.
- The **draw pile** and **picked-up value pile** are shown at the bottom.
- Click **Draw** to draw from the face-down draw pile.
- Click **New Game** at any time to reshuffle and start over without restarting the app; the app asks for confirmation before discarding the current run.
- Drawing automatically returns any currently held pile, then picks up the pile matching the drawn card value and places the drawn card on the bottom.
- To play a card to a tableau:
  - Drag a **value pile** to play its top card, or
  - Drag a specific card from the **held pile** panel,
  - Then drop it onto the target **tableau**.
- When the game ends, the dialog also offers a **New Game** option for immediately starting another run.

Tableau order:
- `1-4`: build up (`A→K`) for Clubs, Diamonds, Hearts, Spades
- `5-8`: build down (`K→A`) for Clubs, Diamonds, Hearts, Spades

## Card images

The UI supports card images named as:

```text
[suit]_[value].png
```

Examples: `hearts_a.png`, `clubs_10.png`, `spades_k.png`.

When built with Maven, the generated jar bundles the `cards/` images and loads them from the classpath automatically. During local development, the UI can still fall back to loading from `cards/` or the project root.

Empty tableau piles can additionally use `[suit]_empty.png` images, such as `clubs_empty.png` or `hearts_empty.png`.

If an image is missing, the UI falls back to text labels.
