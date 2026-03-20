public enum Rank {
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
