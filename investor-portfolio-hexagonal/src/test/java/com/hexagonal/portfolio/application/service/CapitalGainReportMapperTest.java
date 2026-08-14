package com.hexagonal.portfolio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse;
import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse.SchemeCapitalGainDto;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;

/**
 * Tests for {@link CapitalGainReportMapper}, which groups realised capital-gain rows by scheme and
 * folio and splits them into equity and debt buckets.
 *
 * <p>The classification is the part worth pinning. Older rows arrive with no category at all, so
 * the mapper falls back to a gain-shape heuristic; misclassifying there moves a holding between two
 * buckets that are taxed differently.
 */
@DisplayName("CapitalGainReportMapper")
class CapitalGainReportMapperTest {

    private final CapitalGainReportMapper mapper = new CapitalGainReportMapper();

    @Nested
    @DisplayName("empty input")
    class EmptyInput {

        @Test
        @DisplayName("reports 204 with zeroed totals for an empty list")
        void emptyListIsNoContent() {
            CapitalGainReportApiResponse response = mapper.convert(List.of());

            assertThat(response.getStatus()).isEqualTo(204);
            assertThat(response.getStatusMsg()).isEqualTo("No Content");
            assertThat(response.getMsg()).isEqualTo("No capital gain data found");
            assertThat(response.getEquitySummaryList()).isEmpty();
            assertThat(response.getDebtSummaryList()).isEmpty();
            assertThat(response.getEqGain()).isZero();
            assertThat(response.getDebtGain()).isZero();
        }

        @Test
        @DisplayName("treats a null list the same as an empty one")
        void nullListIsNoContent() {
            assertThat(mapper.convert(null).getStatus()).isEqualTo(204);
        }
    }

    @Nested
    @DisplayName("equity / debt classification")
    class Classification {

        @Test
        @DisplayName("honours an explicit EQUITY category, whatever its case")
        void explicitEquityCategory() {
            CapitalGainReportApiResponse response = mapper.convert(List.of(
                    row("HDFC Top 100", "F1", "equity", 1000.0, 0.0)));

            assertThat(response.getEquitySummaryList()).hasSize(1);
            assertThat(response.getDebtSummaryList()).isEmpty();
        }

        @Test
        @DisplayName("routes any other explicit category to debt")
        void explicitNonEquityCategoryIsDebt() {
            CapitalGainReportApiResponse response = mapper.convert(List.of(
                    row("HDFC Liquid", "F2", "DEBT", 0.0, 500.0)));

            assertThat(response.getDebtSummaryList()).hasSize(1);
            assertThat(response.getEquitySummaryList()).isEmpty();
        }

        @Test
        @DisplayName("treats a hybrid-taxed row as equity when the category is blank")
        void hybridTaxationIsEquity() {
            InvestorSchemeWiseTransactionTaxReport r = row("Hybrid Fund", "F3", "  ", 0.0, 200.0);
            r.setHybrid_taxation(true);

            assertThat(mapper.convert(List.of(r)).getEquitySummaryList()).hasSize(1);
        }

        @Test
        @DisplayName("infers equity from a long-term gain when the category is missing")
        void longTermGainInfersEquity() {
            CapitalGainReportApiResponse response = mapper.convert(List.of(
                    row("Unknown Fund", "F4", null, 2500.0, 0.0)));

            assertThat(response.getEquitySummaryList()).hasSize(1);
        }

        @Test
        @DisplayName("infers debt from a short-term-only gain when the category is missing")
        void shortTermOnlyInfersDebt() {
            CapitalGainReportApiResponse response = mapper.convert(List.of(
                    row("Unknown Fund", "F5", null, 0.0, 900.0)));

            assertThat(response.getDebtSummaryList()).hasSize(1);
        }

        @Test
        @DisplayName("defaults to equity when there is no gain data to go on")
        void noGainDataDefaultsToEquity() {
            CapitalGainReportApiResponse response = mapper.convert(List.of(
                    row("Unknown Fund", "F6", null, 0.0, 0.0)));

            assertThat(response.getEquitySummaryList()).hasSize(1);
        }

        /**
         * The heuristic keys on a non-zero net, so a long-term <em>loss</em> classifies as equity
         * just as a gain does.
         */
        @Test
        @DisplayName("classifies a long-term loss as equity, same as a gain")
        void longTermLossIsAlsoEquity() {
            InvestorSchemeWiseTransactionTaxReport r = row("Unknown Fund", "F7", null, 0.0, 0.0);
            r.setLtg_loss(1500.0);

            assertThat(mapper.convert(List.of(r)).getEquitySummaryList()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("grouping")
    class Grouping {

        @Test
        @DisplayName("groups rows sharing a scheme and folio into one summary")
        void groupsBySchemeAndFolio() {
            List<SchemeCapitalGainDto> summaries = mapper.convert(List.of(
                    row("HDFC Top 100", "F1", "EQUITY", 1000.0, 0.0),
                    row("HDFC Top 100", "F1", "EQUITY", 500.0, 0.0)))
                    .getEquitySummaryList();

            assertThat(summaries).hasSize(1);
            assertThat(summaries.get(0).getTransactionCount()).isEqualTo(2);
            assertThat(summaries.get(0).getLtgGain()).isEqualTo(1500.0);
            assertThat(summaries.get(0).getTransactionList()).hasSize(2);
        }

        @Test
        @DisplayName("keeps the same scheme in different folios apart")
        void separatesFolios() {
            assertThat(mapper.convert(List.of(
                    row("HDFC Top 100", "F1", "EQUITY", 1000.0, 0.0),
                    row("HDFC Top 100", "F2", "EQUITY", 500.0, 0.0)))
                    .getEquitySummaryList())
                    .hasSize(2);
        }

        @Test
        @DisplayName("groups a null scheme and folio rather than dropping the row")
        void groupsNullKeys() {
            assertThat(mapper.convert(List.of(
                    row(null, null, "EQUITY", 100.0, 0.0),
                    row(null, null, "EQUITY", 200.0, 0.0)))
                    .getEquitySummaryList())
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("gain arithmetic")
    class GainArithmetic {

        @Test
        @DisplayName("nets gains against losses across both terms")
        void netsGainsAgainstLosses() {
            InvestorSchemeWiseTransactionTaxReport r = row("Fund", "F1", "EQUITY", 5000.0, 2000.0);
            r.setLtg_loss(1000.0);
            r.setStg_loss(500.0);

            SchemeCapitalGainDto summary = mapper.convert(List.of(r)).getEquitySummaryList().get(0);

            // 5000 - 1000 + 2000 - 500
            assertThat(summary.getGainLoss()).isEqualTo(5500.0);
        }

        @Test
        @DisplayName("rolls scheme summaries up into the category total")
        void rollsUpToCategoryTotal() {
            CapitalGainReportApiResponse response = mapper.convert(List.of(
                    row("Fund A", "F1", "EQUITY", 1000.0, 0.0),
                    row("Fund B", "F2", "EQUITY", 2000.0, 0.0),
                    row("Fund C", "F3", "DEBT", 0.0, 750.0)));

            assertThat(response.getEqLtgGain()).isEqualTo(3000.0);
            assertThat(response.getEqGain()).isEqualTo(3000.0);
            assertThat(response.getDebtStgGain()).isEqualTo(750.0);
            assertThat(response.getDebtGain()).isEqualTo(750.0);
        }

        @Test
        @DisplayName("treats null gain fields as zero rather than failing")
        void nullGainsAreZero() {
            InvestorSchemeWiseTransactionTaxReport r = new InvestorSchemeWiseTransactionTaxReport();
            r.setScheme("Sparse Fund");
            r.setFolio("F1");
            r.setCategory("EQUITY");

            SchemeCapitalGainDto summary = mapper.convert(List.of(r)).getEquitySummaryList().get(0);

            assertThat(summary.getGainLoss()).isZero();
            assertThat(summary.getStgGain()).isZero();
        }

        /**
         * When the pre-split stg/ltg amounts are absent the mapper substitutes the row's overall
         * purchase and sold amounts, routing them to whichever bucket has a non-zero gain.
         */
        @Test
        @DisplayName("substitutes overall amounts when the term-split amounts are missing")
        void substitutesAmountsWhenSplitIsMissing() {
            InvestorSchemeWiseTransactionTaxReport r = row("Fund", "F1", "EQUITY", 1200.0, 0.0);
            r.setPurchase_amount(10000.0);
            r.setSold_amount(11200.0);

            SchemeCapitalGainDto summary = mapper.convert(List.of(r)).getEquitySummaryList().get(0);

            assertThat(summary.getLtgPurchaseAmount()).isEqualTo(10000.0);
            assertThat(summary.getLtgSoldAmount()).isEqualTo(11200.0);
            assertThat(summary.getStgPurchaseAmount())
                    .as("no short-term gain, so nothing is routed to the short-term bucket")
                    .isZero();
        }

        @Test
        @DisplayName("prefers the term-split amounts when they are present")
        void prefersExplicitSplitAmounts() {
            InvestorSchemeWiseTransactionTaxReport r = row("Fund", "F1", "EQUITY", 1200.0, 0.0);
            r.setPurchase_amount(10000.0);
            r.setLtg_purchase_amount(9000.0);
            r.setLtg_sold_amount(10200.0);

            SchemeCapitalGainDto summary = mapper.convert(List.of(r)).getEquitySummaryList().get(0);

            assertThat(summary.getLtgPurchaseAmount()).isEqualTo(9000.0);
            assertThat(summary.getLtgSoldAmount()).isEqualTo(10200.0);
        }

        @Test
        @DisplayName("accumulates indexed-cost figures used for debt indexation")
        void accumulatesIndexedFigures() {
            InvestorSchemeWiseTransactionTaxReport r = row("Debt Fund", "F1", "DEBT", 0.0, 0.0);
            r.setIndexed_cost(11500.0);
            r.setIndexed_gain(800.0);
            r.setIndexed_sold_amount(12300.0);

            SchemeCapitalGainDto summary = mapper.convert(List.of(r)).getDebtSummaryList().get(0);

            assertThat(summary.getIndexedCost()).isCloseTo(11500.0, within(0.001));
            assertThat(summary.getIndexedGain()).isCloseTo(800.0, within(0.001));
            assertThat(summary.getIndexedSoldAmount()).isCloseTo(12300.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("AMC logo resolution")
    class LogoResolution {

        @Test
        @DisplayName("uses the row's logo when it is a real URL")
        void usesRealUrl() {
            InvestorSchemeWiseTransactionTaxReport r = row("Fund", "F1", "EQUITY", 100.0, 0.0);
            r.setAmc_logo("https://cdn.example.com/hdfc.png");

            assertThat(mapper.convert(List.of(r)).getEquitySummaryList().get(0).getAmcLogo())
                    .isEqualTo("https://cdn.example.com/hdfc.png");
        }

        /**
         * The raw field often holds the scheme name rather than a URL, which is why it is only
         * trusted when it looks like one.
         */
        @Test
        @DisplayName("ignores a non-URL logo field and resolves from the scheme name")
        void ignoresNonUrlLogo() {
            InvestorSchemeWiseTransactionTaxReport r = row("Axis Bluechip Fund", "F1", "EQUITY", 100.0, 0.0);
            r.setAmc_logo("Axis Bluechip Fund");

            assertThat(mapper.convert(List.of(r)).getEquitySummaryList().get(0).getAmcLogo())
                    .endsWith("axis.png");
        }

        @Test
        @DisplayName("resolves from the scheme name when no logo is set at all")
        void resolvesFromSchemeName() {
            assertThat(mapper.convert(List.of(row("ICICI Prudential Value", "F1", "EQUITY", 100.0, 0.0)))
                    .getEquitySummaryList().get(0).getAmcLogo())
                    .endsWith("icici.png");
        }
    }

    // ---------------------------------------------------------------------

    private static InvestorSchemeWiseTransactionTaxReport row(
            String scheme, String folio, String category, Double ltgGain, Double stgGain) {
        InvestorSchemeWiseTransactionTaxReport r = new InvestorSchemeWiseTransactionTaxReport();
        r.setScheme(scheme);
        r.setFolio(folio);
        r.setCategory(category);
        r.setLtg_gain(ltgGain);
        r.setStg_gain(stgGain);
        return r;
    }
}
