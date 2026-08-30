package com.global.ct.frameinventory.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogValueSanitizerTest {

    @Test
    void replacesControlAndFormattingCharacters() {
        String value = "safe\r\n\t\u0000\u001B\u007F\u0085\u2028\u2029\u202Eend";

        assertThat(LogValueSanitizer.sanitize(value)).isEqualTo("safe__________end");
    }

    @Test
    void truncatesLongValuesToTheMaximumLength() {
        String sanitized = LogValueSanitizer.sanitize("a".repeat(300));

        assertThat(sanitized)
            .hasSize(LogValueSanitizer.MAX_LENGTH)
            .endsWith("...");
    }

    @Test
    void representsNullWithoutLoggingTheLiteralValue() {
        assertThat(LogValueSanitizer.sanitize(null)).isEqualTo("<null>");
    }
}
