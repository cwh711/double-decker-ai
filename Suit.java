public enum Suit {
    CLUBS("clubs", "♣"),
    DIAMONDS("diamonds", "♦"),
    HEARTS("hearts", "♥"),
    SPADES("spades", "♠");

    final String fileName;
    final String symbol;

    Suit(String fileName, String symbol) {
        this.fileName = fileName;
        this.symbol = symbol;
    }
}
