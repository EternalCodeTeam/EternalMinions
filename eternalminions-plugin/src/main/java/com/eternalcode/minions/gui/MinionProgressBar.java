package com.eternalcode.minions.gui;

final class MinionProgressBar {

    private static final String FILLED_COLOR = "<green>";
    private static final String EMPTY_COLOR = "<dark_gray>";
    private static final String SEGMENT = "■";

    private MinionProgressBar() {
    }

    static String render(long progress, long requiredProgress, int segments) {
        if (progress < 0L) {
            throw new IllegalArgumentException("Progress cannot be negative");
        }
        if (requiredProgress <= 0L) {
            throw new IllegalArgumentException("Required progress must be positive");
        }
        if (segments <= 0) {
            throw new IllegalArgumentException("Progress bar must contain at least one segment");
        }

        double completion = Math.min(1.0D, (double) progress / requiredProgress);
        int filledSegments = (int) Math.floor(completion * segments);
        return renderSegments(filledSegments, segments);
    }

    static String renderMaximum(int segments) {
        if (segments <= 0) {
            throw new IllegalArgumentException("Progress bar must contain at least one segment");
        }

        return renderSegments(segments, segments);
    }

    private static String renderSegments(int filledSegments, int segments) {
        return FILLED_COLOR
            + SEGMENT.repeat(filledSegments)
            + EMPTY_COLOR
            + SEGMENT.repeat(segments - filledSegments);
    }
}
