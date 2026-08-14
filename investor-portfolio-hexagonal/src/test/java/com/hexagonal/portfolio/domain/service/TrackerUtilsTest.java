package com.hexagonal.portfolio.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link TrackerUtils} — day counting for holding periods, and the segregated-portfolio
 * NAV haircut applied when a scheme has written down defaulted debt.
 */
@DisplayName("TrackerUtils")
class TrackerUtilsTest {

    @Nested
    @DisplayName("getDaysBetweenDates")
    class DaysBetweenDates {

        @Test
        @DisplayName("counts whole days forward")
        void countsForward() {
            Date start = new Date(0);
            Date end = new Date(TimeUnit.DAYS.toMillis(365));

            assertThat(TrackerUtils.getDaysBetweenDates(start, end)).isEqualTo(365);
        }

        @Test
        @DisplayName("is zero for the same instant")
        void sameInstantIsZero() {
            Date instant = new Date(1_600_000_000_000L);

            assertThat(TrackerUtils.getDaysBetweenDates(instant, instant)).isZero();
        }

        /**
         * Unlike {@code XIRR.getDateDiff}, this one is signed — an end date before the start yields
         * a negative count. Holding-period logic depends on that, so it is pinned deliberately.
         */
        @Test
        @DisplayName("is signed, unlike XIRR's absolute variant")
        void isSigned() {
            Date start = new Date(TimeUnit.DAYS.toMillis(10));
            Date end = new Date(0);

            assertThat(TrackerUtils.getDaysBetweenDates(start, end)).isEqualTo(-10);
        }

        @Test
        @DisplayName("truncates a partial day rather than rounding")
        void truncatesPartialDays() {
            Date start = new Date(0);
            Date end = new Date(TimeUnit.HOURS.toMillis(47));

            assertThat(TrackerUtils.getDaysBetweenDates(start, end)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("segregatedSchemes")
    class SegregatedSchemes {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "RMFCPGP,  1.45",
                "RMFSHGP,  0.10",
                "RMFSDGP,  0.56",
                "RMFESGP,  1.45",
                "B321G,    5.62",
                "B303G,    7.54",
                "B380B,    3.69",
                "TFG1,     1.6548",
                "TFWS,     73.3877",
                "TIAG,     5.1525",
                "TTMHG,    15.0153",
                "FTI104,   1.75",
                "FTI155,   0.91",
                "FTI406,   1.55",
                "10803GP,  2.96",
                "10805GP,  1.961",
                "10804GP,  1.355",
                "108COGP,  72.9384",
        })
        @DisplayName("returns the written-down percentage for a segregated scheme")
        void returnsHaircutForKnownScheme(String schemeCode, String expected) {
            assertThat(TrackerUtils.segregatedSchemes(schemeCode)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" has no haircut")
        @ValueSource(strings = {"HDFC001", "UNKNOWN", "", "rmfcpgp"})
        @DisplayName("returns an empty string for a scheme with no segregated portfolio")
        void returnsEmptyForUnknownScheme(String schemeCode) {
            assertThat(TrackerUtils.segregatedSchemes(schemeCode)).isEmpty();
        }

        /**
         * Lookup is exact and case-sensitive: {@code rmfcpgp} does not match {@code RMFCPGP}. If a
         * caller ever lower-cases a scheme code before this call, the haircut is silently skipped
         * and the valuation overstates the holding.
         */
        @Test
        @DisplayName("matching is case-sensitive")
        void matchingIsCaseSensitive() {
            assertThat(TrackerUtils.segregatedSchemes("RMFCPGP")).isEqualTo("1.45");
            assertThat(TrackerUtils.segregatedSchemes("rmfcpgp")).isEmpty();
        }

        @Test
        @DisplayName("a null scheme code is absorbed rather than thrown")
        void nullSchemeCodeIsAbsorbed() {
            assertThat(TrackerUtils.segregatedSchemes(null)).isEmpty();
        }

        @Test
        @DisplayName("commented-out schemes are genuinely absent")
        void commentedOutSchemesAreAbsent() {
            // These were deliberately disabled in the legacy source; they must not resolve.
            assertThat(TrackerUtils.segregatedSchemes("RMFMIGP")).isEmpty();
            assertThat(TrackerUtils.segregatedSchemes("RMFCBGP")).isEmpty();
            assertThat(TrackerUtils.segregatedSchemes("TFG1S")).isEmpty();
            assertThat(TrackerUtils.segregatedSchemes("TTMHGS")).isEmpty();
        }
    }
}
