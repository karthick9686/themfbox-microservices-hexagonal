package com.hexagonal.portfolio.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;
import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;

/**
 * Tests for {@link TransactionDataUtils}, which cancels reversal pairs before a portfolio is
 * valued.
 *
 * <p>This matters more than its size suggests. A reversal that is not cancelled leaves both the
 * original purchase and its negative twin in the transaction stream, and every downstream figure —
 * units, cost basis, XIRR — is computed from that stream.
 */
@DisplayName("TransactionDataUtils")
class TransactionDataUtilsTest {

    private static final Date TRADE_DAY = new Date(TimeUnit.DAYS.toMillis(1000));
    private static final Date OTHER_DAY = new Date(TimeUnit.DAYS.toMillis(1001));

    @Nested
    @DisplayName("CAMS reversals")
    class CamsReversals {

        @Test
        @DisplayName("cancels a purchase against its same-day negative twin")
        void cancelsMatchingPair() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, 100.0, "Purchase"),
                    cams(2, TRADE_DAY, -100.0, "Purchase"),
                    cams(3, TRADE_DAY, 50.0, "Purchase")));

            List<InvestorTransactionCams> result = TransactionDataUtils.removeCamsMinusTransaction(list);

            assertThat(result).extracting(InvestorTransactionCams::getId).containsExactly(3);
        }

        @Test
        @DisplayName("keeps a reversal whose twin is on a different date")
        void keepsUnmatchedByDate() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, 100.0, "Purchase"),
                    cams(2, OTHER_DAY, -100.0, "Purchase")));

            List<InvestorTransactionCams> result = TransactionDataUtils.removeCamsMinusTransaction(list);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("keeps a reversal whose twin has a different transaction type")
        void keepsUnmatchedByType() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, 100.0, "Purchase"),
                    cams(2, TRADE_DAY, -100.0, "Redemption")));

            List<InvestorTransactionCams> result = TransactionDataUtils.removeCamsMinusTransaction(list);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("keeps a reversal whose twin has a different unit count")
        void keepsUnmatchedByUnits() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, 100.0, "Purchase"),
                    cams(2, TRADE_DAY, -99.0, "Purchase")));

            List<InvestorTransactionCams> result = TransactionDataUtils.removeCamsMinusTransaction(list);

            assertThat(result).hasSize(2);
        }

        /**
         * The "SIP Rejection" branch tests {@code transaction_type} — the rejection's own type —
         * against the two systematic-purchase literals, so the condition is never true and the
         * rejection is never cancelled. It reads as though it meant to test the candidate row's
         * type ({@code trxn.getTrxn_type_()}). Pinned as it behaves today, not as it appears to
         * intend; see the note in the review write-up.
         */
        @Test
        @DisplayName("never cancels a SIP Rejection, because the branch tests the wrong operand")
        void sipRejectionIsNeverCancelled() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, 100.0, "Fresh Purchase Systematic"),
                    cams(2, TRADE_DAY, -100.0, "SIP Rejection")));

            List<InvestorTransactionCams> result = TransactionDataUtils.removeCamsMinusTransaction(list);

            assertThat(result)
                    .as("both rows survive; the systematic purchase is still counted")
                    .hasSize(2);
        }

        @Test
        @DisplayName("leaves an all-positive list untouched")
        void leavesPositiveListAlone() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, 100.0, "Purchase"),
                    cams(2, OTHER_DAY, 50.0, "Purchase")));

            assertThat(TransactionDataUtils.removeCamsMinusTransaction(list)).hasSize(2);
        }

        @Test
        @DisplayName("handles an empty list")
        void handlesEmptyList() {
            assertThat(TransactionDataUtils.removeCamsMinusTransaction(new ArrayList<>())).isEmpty();
        }

        @Test
        @DisplayName("absorbs a null unit count rather than propagating the failure")
        void absorbsNullUnits() {
            List<InvestorTransactionCams> list = new ArrayList<>(List.of(
                    cams(1, TRADE_DAY, null, "Purchase")));

            assertThat(TransactionDataUtils.removeCamsMinusTransaction(list)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Karvy reversals")
    class KarvyReversals {

        @Test
        @DisplayName("cancels a purchase against its same-day negative twin")
        void cancelsMatchingPair() {
            List<InvestorTransactionKarvy> list = new ArrayList<>(List.of(
                    karvy(1, TRADE_DAY, 100.0, "IH1"),
                    karvy(2, TRADE_DAY, -100.0, "IH1"),
                    karvy(3, TRADE_DAY, 25.0, "IH2")));

            List<InvestorTransactionKarvy> result = TransactionDataUtils.removeKarvyMinusTransaction(list);

            assertThat(result).extracting(InvestorTransactionKarvy::getId).containsExactly(3);
        }

        /**
         * Karvy also splits one purchase across several rows sharing an instrument number, so a
         * single reversal can cancel a group whose units sum to it.
         */
        @Test
        @DisplayName("cancels a reversal against a group of same-instrument rows that sum to it")
        void cancelsAgainstSummedGroup() {
            List<InvestorTransactionKarvy> list = new ArrayList<>(List.of(
                    karvy(1, TRADE_DAY, 60.0, "IH1"),
                    karvy(2, TRADE_DAY, 40.0, "IH1"),
                    karvy(3, TRADE_DAY, -100.0, "IH1"),
                    karvy(4, TRADE_DAY, 10.0, "IH9")));

            List<InvestorTransactionKarvy> result = TransactionDataUtils.removeKarvyMinusTransaction(list);

            assertThat(result).extracting(InvestorTransactionKarvy::getId).containsExactly(4);
        }

        @Test
        @DisplayName("keeps the group when the units do not sum to the reversal")
        void keepsGroupThatDoesNotSum() {
            List<InvestorTransactionKarvy> list = new ArrayList<>(List.of(
                    karvy(1, TRADE_DAY, 60.0, "IH1"),
                    karvy(2, TRADE_DAY, 30.0, "IH1"),
                    karvy(3, TRADE_DAY, -100.0, "IH1")));

            List<InvestorTransactionKarvy> result = TransactionDataUtils.removeKarvyMinusTransaction(list);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("does not group rows from a different instrument number")
        void doesNotGroupAcrossInstrumentNumbers() {
            List<InvestorTransactionKarvy> list = new ArrayList<>(List.of(
                    karvy(1, TRADE_DAY, 60.0, "IH1"),
                    karvy(2, TRADE_DAY, 40.0, "IH2"),
                    karvy(3, TRADE_DAY, -100.0, "IH1")));

            List<InvestorTransactionKarvy> result = TransactionDataUtils.removeKarvyMinusTransaction(list);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("ignores transaction type, unlike the CAMS variant")
        void ignoresTransactionType() {
            List<InvestorTransactionKarvy> list = new ArrayList<>(List.of(
                    karvyTyped(1, TRADE_DAY, 100.0, "IH1", "Purchase"),
                    karvyTyped(2, TRADE_DAY, -100.0, "IH1", "Redemption")));

            assertThat(TransactionDataUtils.removeKarvyMinusTransaction(list)).isEmpty();
        }

        @Test
        @DisplayName("handles an empty list")
        void handlesEmptyList() {
            assertThat(TransactionDataUtils.removeKarvyMinusTransaction(new ArrayList<>())).isEmpty();
        }
    }

    private static InvestorTransactionCams cams(int id, Date date, Double units, String type) {
        InvestorTransactionCams t = new InvestorTransactionCams();
        t.setId(id);
        t.setTraddate(date);
        t.setUnits(units);
        t.setTrxn_type_(type);
        return t;
    }

    private static InvestorTransactionKarvy karvy(int id, Date date, Double units, String ihno) {
        return karvyTyped(id, date, units, ihno, "Purchase");
    }

    private static InvestorTransactionKarvy karvyTyped(
            int id, Date date, Double units, String ihno, String description) {
        InvestorTransactionKarvy t = new InvestorTransactionKarvy();
        t.setId(id);
        t.setTransaction_date(date);
        t.setUnits(units);
        t.setIhno(ihno);
        t.setTransaction_description(description);
        return t;
    }
}
