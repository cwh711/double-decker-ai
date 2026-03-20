public record DragSelection(Rank valueRank, Integer heldIndex) {
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
