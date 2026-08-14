package com.hexagonal.portfolio.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link MyMFBoxUtils} — Indian-format currency grouping, parameter normalisation, and
 * AMC logo resolution.
 */
@DisplayName("MyMFBoxUtils")
class MyMFBoxUtilsTest {

    @Nested
    @DisplayName("formatInRupees")
    class FormatInRupees {

        /**
         * The Indian grouping system is not the western one: after the last three digits it groups
         * in pairs, so ten million is 1,00,00,000 rather than 10,000,000.
         */
        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "0,             0",
                "9,             9",
                "999,           999",
                "1000,          '1,000'",
                "99999,         '99,999'",
                "100000,        '1,00,000'",
                "1000000,       '10,00,000'",
                "10000000,      '1,00,00,000'",
                "1234567890,    '1,23,45,67,890'",
        })
        @DisplayName("groups digits in the Indian lakh/crore style")
        void groupsIndianStyle(long amount, String expected) {
            assertThat(MyMFBoxUtils.formatInRupees(amount)).isEqualTo(expected);
        }

        @Test
        @DisplayName("leaves three digits or fewer untouched")
        void shortNumbersAreUnchanged() {
            assertThat(MyMFBoxUtils.formatInRupees(42)).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("checkParem")
    class CheckParem {

        @ParameterizedTest(name = "\"{0}\" normalises to empty")
        @ValueSource(strings = {"null", "NULL", "Null", "undefined", "UNDEFINED", "  null  ", "   "})
        @DisplayName("treats placeholder literals and blanks as empty")
        void normalisesPlaceholders(String raw) {
            assertThat(MyMFBoxUtils.checkParem(raw)).isEmpty();
        }

        @Test
        @DisplayName("treats a null reference as empty")
        void nullBecomesEmpty() {
            assertThat(MyMFBoxUtils.checkParem(null)).isEmpty();
        }

        @Test
        @DisplayName("trims a real value rather than discarding it")
        void trimsRealValues() {
            assertThat(MyMFBoxUtils.checkParem("  HDFC  ")).isEqualTo("HDFC");
        }

        @Test
        @DisplayName("keeps a value that merely contains a placeholder word")
        void keepsValuesContainingPlaceholders() {
            assertThat(MyMFBoxUtils.checkParem("nullable fund")).isEqualTo("nullable fund");
        }
    }

    @Nested
    @DisplayName("getLogoByAmcNameOrSchemeName")
    class LogoResolution {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "'Axis Bluechip Fund',                      axis.png",
                "'HDFC Top 100',                            hdfc.png",
                "'ICICI Prudential Value Discovery',        icici.png",
                "'Aditya Birla Sun Life Frontline Equity',  birla.png",
                "'ABSL Corporate Bond',                     birla.png",
                "'Bandhan Core Equity',                     bandhan.png",
                "'Baroda BNP Paribas Large Cap',            bnp.png",
        })
        @DisplayName("resolves the logo from the first word of the name")
        void resolvesFromFirstWord(String name, String expectedLogo) {
            assertThat(MyMFBoxUtils.getLogoByAmcNameOrSchemeName(name)).isEqualTo(expectedLogo);
        }

        @Test
        @DisplayName("is case-insensitive on the first word")
        void isCaseInsensitive() {
            assertThat(MyMFBoxUtils.getLogoByAmcNameOrSchemeName("AXIS Bluechip"))
                    .isEqualTo(MyMFBoxUtils.getLogoByAmcNameOrSchemeName("axis bluechip"));
        }

        @Test
        @DisplayName("tolerates irregular whitespace between words")
        void toleratesIrregularWhitespace() {
            assertThat(MyMFBoxUtils.getLogoByAmcNameOrSchemeName("  hdfc    top   100  "))
                    .isEqualTo("hdfc.png");
        }

        @ParameterizedTest(name = "\"{0}\" falls back to empty.png")
        @ValueSource(strings = {"Unknown Fund House", "zzz", "null", "undefined", "   "})
        @DisplayName("falls back to a placeholder for an unmapped or blank name")
        void fallsBackToPlaceholder(String name) {
            assertThat(MyMFBoxUtils.getLogoByAmcNameOrSchemeName(name)).isEqualTo("empty.png");
        }

        @Test
        @DisplayName("falls back to a placeholder for a null name")
        void nullFallsBackToPlaceholder() {
            assertThat(MyMFBoxUtils.getLogoByAmcNameOrSchemeName(null)).isEqualTo("empty.png");
        }
    }
}
