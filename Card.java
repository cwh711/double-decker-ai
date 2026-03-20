public record Card(Suit suit, Rank rank) {
    String imageFileName() {
        return suit.fileName + "_" + rank.fileName + ".png";
    }

    @Override
    public String toString() {
        return rank.label + suit.symbol;
    }
}
