# double-decker-ai

Console implementation of the requested two-deck solitaire variant in Java.

## Run

```bash
javac SolitaireGame.java
java SolitaireGame
```

Optionally pass a numeric seed for deterministic shuffles:

```bash
java SolitaireGame 42
```

## Commands

- `draw`
- `play pile <rank> <tableau#>`
- `play held <positionFromTop> <tableau#>`
- `show`
- `help`
- `quit`

Tableau numbers:
- `1-4`: build up (`A→K`) for Clubs, Diamonds, Hearts, Spades
- `5-8`: build down (`K→A`) for Clubs, Diamonds, Hearts, Spades
