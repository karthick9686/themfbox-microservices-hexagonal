package com.hexagonal.portfolio.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the domain rules the web adapter leans on.
 *
 * <p>{@code checkParem} is what turns the literal strings a browser sends — {@code "null"},
 * {@code "undefined"} — into empty values, so every branch decision in the controller
 * depends on it behaving exactly as the legacy implementation did.
 */
class MfboxUtilsTest {

    @ParameterizedTest(name = "checkParem(\"{0}\") is empty")
    @ValueSource(strings = {"null", "NULL", "Null", "undefined", "UNDEFINED", "Undefined",
            "  null  ", "  undefined  ", "   ", ""})
    @DisplayName("placeholder and blank values collapse to an empty string")
    void collapsesPlaceholders(String input) {
        assertThat(MfboxUtils.checkParem(input)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("null collapses to an empty string rather than throwing")
    void nullCollapses(String input) {
        assertThat(MfboxUtils.checkParem(input)).isEmpty();
    }

    @ParameterizedTest(name = "checkParem(\"{0}\") -> \"{1}\"")
    @CsvSource({
            "'  Live  ', 'Live'",
            "'2023-2024', '2023-2024'",
            "'  acme', 'acme'",
            "'holding-report  ', 'holding-report'"
    })
    @DisplayName("real values are trimmed but otherwise preserved")
    void trimsRealValues(String input, String expected) {
        assertThat(MfboxUtils.checkParem(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("a value merely containing \"null\" is left alone")
    void doesNotCollapseSubstringMatches() {
        assertThat(MfboxUtils.checkParem("nullable")).isEqualTo("nullable");
        assertThat(MfboxUtils.checkParem("annulled")).isEqualTo("annulled");
    }

    @Test
    @DisplayName("segregated schemes resolve to their published haircut percentage")
    void resolvesSegregatedSchemes() {
        assertThat(MfboxUtils.segregatedSchemes("RMFCPGP")).isEqualTo("1.45");
        assertThat(MfboxUtils.segregatedSchemes("B303G")).isEqualTo("7.54");
        assertThat(MfboxUtils.segregatedSchemes("108COGP")).isEqualTo("72.9384");
    }

    @Test
    @DisplayName("an unknown scheme code yields an empty haircut, not null")
    void unknownSegregatedSchemeIsEmpty() {
        assertThat(MfboxUtils.segregatedSchemes("NOT-A-SCHEME")).isEmpty();
    }

    @Test
    @DisplayName("only the configured advisors are flagged for multi-ARN handling")
    void flagsMultiArnAdvisors() {
        assertThat(MfboxUtils.getSameSchemeMultipleArnAdvisors("mutualfundsaving")).isTrue();
        assertThat(MfboxUtils.getSameSchemeMultipleArnAdvisors("bijuyohannan")).isTrue();
        assertThat(MfboxUtils.getSameSchemeMultipleArnAdvisors("acme")).isFalse();
        assertThat(MfboxUtils.getSameSchemeMultipleArnAdvisors(null)).isFalse();
    }

    @Test
    @DisplayName("the Karvy neutral-dividend vocabulary is carried over intact")
    void exposesKarvyNeutralDividendTypes() {
        assertThat(MfboxUtils.KarvyNeutralDividendTransactionType())
                .containsExactly(
                        "Gross Dividend",
                        "Gross Dividend Rejection",
                        "Gross Dividend Rejection Reversal",
                        "Dividend Sweep Out",
                        "Dividend Sweep Out Rej",
                        "Dividend Sweep Out Rej.");
    }
}
