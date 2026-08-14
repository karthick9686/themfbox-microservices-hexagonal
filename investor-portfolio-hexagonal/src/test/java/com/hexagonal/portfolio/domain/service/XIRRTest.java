package com.hexagonal.portfolio.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the XIRR solver.
 *
 * <p>This is the highest-risk arithmetic in the service: every returned annualised return figure
 * comes through here, and both solvers answer {@code 0} when they fail to converge — a value
 * indistinguishable from a genuine 0% return. The cases below pin the convergent results, and
 * pin which inputs silently produce that 0.
 *
 * <p>Sign convention: {@code days[0]} is the reference date and cash flows are discounted back to
 * it, so an investment is negative and a redemption positive.
 */
@DisplayName("XIRR")
class XIRRTest {

    private static final double TOLERANCE = 1e-4;

    @Nested
    @DisplayName("Newton's method")
    class NewtonsMethod {

        @ParameterizedTest(name = "{0} invested, {1} returned after {2} days -> {3}")
        @CsvSource({
                "-10000, 11000,  365, 0.10",
                "-10000, 12000,  365, 0.20",
                "-10000, 10000,  365, 0.00",
                "-10000,  9000,  365, -0.10",
                "-10000, 12100,  730, 0.10",
        })
        @DisplayName("solves a single invest-then-redeem cash flow")
        void solvesSimpleCashFlow(double invested, double returned, double days, double expected) {
            double result = XIRR.Newtons_method(
                    new double[]{invested, returned}, new double[]{0, days}, 0.1);

            assertThat(result).isCloseTo(expected, within(TOLERANCE));
        }

        @Test
        @DisplayName("solves a multi-instalment SIP-shaped cash flow")
        void solvesMultipleContributions() {
            double[] payments = {-1000, -1000, -1000, 3200};
            double[] days = {0, 30, 60, 365};

            double result = XIRR.Newtons_method(payments, days, 0.1);

            // Verify by substitution rather than against a hard-coded rate: the solver's own
            // objective function must be ~0 at the root it returns.
            assertThat(XIRR.total_f_xirr(payments, days, result)).isCloseTo(0.0, within(1e-6));
            assertThat(result).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("returns 0 when no root exists, which is indistinguishable from a 0% return")
        void returnsZeroWhenItCannotConverge() {
            // All-positive flows have no rate at which the discounted sum is zero.
            double result = XIRR.Newtons_method(
                    new double[]{1000, 2000}, new double[]{0, 365}, 0.1);

            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("bisection method")
    class BisectionMethod {

        @Test
        @DisplayName("agrees with Newton's method on a solvable cash flow")
        void agreesWithNewton() {
            double[] payments = {-10000, 11000};
            double[] days = {0, 365};

            assertThat(XIRR.Bisection_method(payments, days, 0.1))
                    .isCloseTo(XIRR.Newtons_method(payments, days, 0.1), within(1e-3));
        }

        @Test
        @DisplayName("short-circuits to 0 when a two-flow pair nets to zero")
        void shortCircuitsOnZeroSumPair() {
            double result = XIRR.Bisection_method(
                    new double[]{-5000, 5000}, new double[]{0, 365}, 0.1);

            assertThat(result).isZero();
        }

        /**
         * The zero-sum short-circuit truncates to an int, so a pair netting to any fraction below
         * 1.0 is also treated as zero — worth pinning because it is not what the code appears to say.
         */
        @Test
        @DisplayName("the zero-sum check truncates, so a sub-rupee net also yields 0")
        void zeroSumCheckTruncates() {
            double result = XIRR.Bisection_method(
                    new double[]{-5000, 5000.99}, new double[]{0, 365}, 0.1);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("returns 0 when no bracket can be found")
        void returnsZeroWithoutBracket() {
            double result = XIRR.Bisection_method(
                    new double[]{1000, 2000, 3000}, new double[]{0, 365, 730}, 0.1);

            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("bracketing")
    class Bracketing {

        @Test
        @DisplayName("brackets a sign change around the root")
        void findsBracketAroundRoot() {
            double[] payments = {-10000, 11000};
            double[] days = {0, 365};

            XIRR.Bracket bracket = XIRR.find_bracket(payments, days, 0.1);

            assertThat(bracket).isNotNull();
            assertThat(XIRR.total_f_xirr(payments, days, bracket.left)
                    * XIRR.total_f_xirr(payments, days, bracket.right))
                    .as("the objective function must change sign across the bracket")
                    .isLessThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("returns null when the function never changes sign")
        void returnsNullWithoutSignChange() {
            assertThat(XIRR.find_bracket(new double[]{1000, 2000}, new double[]{0, 365}, 0.1))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("objective function")
    class ObjectiveFunction {

        @Test
        @DisplayName("leaves the reference-date flow undiscounted")
        void referenceFlowIsUndiscounted() {
            assertThat(XIRR.f_xirr(500, 0, 0, 0.25)).isEqualTo(500.0);
        }

        @Test
        @DisplayName("discounts a later flow back to the reference date")
        void discountsLaterFlow() {
            // One year later at 10%: 1100 / 1.1 = 1000
            assertThat(XIRR.f_xirr(1100, 365, 0, 0.10)).isCloseTo(1000.0, within(TOLERANCE));
        }

        /**
         * A rate at or below -100% would make the base zero or negative and the power undefined,
         * so it is clamped just above -1. Without the clamp this returns -100 instead of a very
         * large positive number.
         */
        @Test
        @DisplayName("clamps a rate at or below -100% instead of producing a negative base")
        void clampsRateAtMinusOne() {
            double result = XIRR.f_xirr(100, 365, 0, -2.0);

            assertThat(result).isGreaterThan(1e6);
        }

        @Test
        @DisplayName("sums each flow's contribution")
        void totalSumsContributions() {
            double[] payments = {-1000, 1100};
            double[] days = {0, 365};

            assertThat(XIRR.total_f_xirr(payments, days, 0.10)).isCloseTo(0.0, within(TOLERANCE));
        }

        @Test
        @DisplayName("the derivative is non-zero where the solver needs it")
        void derivativeIsUsable() {
            assertThat(XIRR.total_df_xirr(new double[]{-1000, 1100}, new double[]{0, 365}, 0.10))
                    .isNotEqualTo(0.0);
        }

        @Test
        @DisplayName("composeFunctions adds")
        void composeAdds() {
            assertThat(XIRR.composeFunctions(2.5, 3.5)).isEqualTo(6.0);
        }
    }

    @Nested
    @DisplayName("date difference")
    class DateDifference {

        @Test
        @DisplayName("counts whole days between two dates")
        void countsDays() {
            Date start = new Date(0);
            Date end = new Date(TimeUnit.DAYS.toMillis(10));

            assertThat(XIRR.getDateDiff(start, end)).isEqualTo(10L);
        }

        @Test
        @DisplayName("is absolute, so argument order does not matter")
        void isAbsolute() {
            Date start = new Date(0);
            Date end = new Date(TimeUnit.DAYS.toMillis(10));

            assertThat(XIRR.getDateDiff(end, start)).isEqualTo(XIRR.getDateDiff(start, end));
        }

        @Test
        @DisplayName("converts to the requested unit")
        void convertsUnits() {
            Date start = new Date(0);
            Date end = new Date(TimeUnit.DAYS.toMillis(2));

            assertThat(XIRR.getDateDiff(start, end, TimeUnit.HOURS)).isEqualTo(48L);
        }

        @Test
        @DisplayName("accepts calendars")
        void acceptsCalendars() {
            Calendar from = Calendar.getInstance();
            from.setTimeInMillis(0);
            Calendar to = Calendar.getInstance();
            to.setTimeInMillis(TimeUnit.DAYS.toMillis(5));

            assertThat(XIRR.getDateDiff(from, to)).isEqualTo(5L);
        }
    }
}
