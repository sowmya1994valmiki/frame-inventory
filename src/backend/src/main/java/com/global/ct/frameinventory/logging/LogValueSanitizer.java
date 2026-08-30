package com.global.ct.frameinventory.logging;

public final class LogValueSanitizer {

    static final int MAX_LENGTH = 256;
    private static final String TRUNCATION_MARKER = "...";

    private LogValueSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return "<null>";
        }

        int codePointCount = value.codePointCount(0, value.length());
        boolean truncated = codePointCount > MAX_LENGTH;
        int contentLength = truncated ? MAX_LENGTH - TRUNCATION_MARKER.length() : MAX_LENGTH;
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), MAX_LENGTH));

        value.codePoints()
            .limit(contentLength)
            .forEach(codePoint -> sanitized.appendCodePoint(
                isUnsafe(codePoint) ? '_' : codePoint
            ));

        if (truncated) {
            sanitized.append(TRUNCATION_MARKER);
        }
        return sanitized.toString();
    }

    private static boolean isUnsafe(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isISOControl(codePoint)
            || type == Character.FORMAT
            || type == Character.LINE_SEPARATOR
            || type == Character.PARAGRAPH_SEPARATOR
            || type == Character.SURROGATE;
    }
}
