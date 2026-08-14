package com.hexagonal.portfolio.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse.BroadCategoryDto;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse.CategoryDto;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse.SchemeDto;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWisePortfolioResponse;

/**
 * Tests for {@link MobilePortfolioMapper}, which reshapes a valued portfolio into the nested
 * broad-category → category → scheme payload the mobile app consumes.
 *
 * <p>The interesting behaviour is not the field copying but the grouping and filtering: which
 * holdings appear at all, how categories collapse into broad categories, and how weights are
 * apportioned.
 */
@DisplayName("MobilePortfolioMapper")
class MobilePortfolioMapperTest {

    private final MobilePortfolioMapper mapper = new MobilePortfolioMapper();

    @Nested
    @DisplayName("portfolio summary")
    class Summary {

        @Test
        @DisplayName("copies the totals and reports a full portfolio weight")
        void copiesTotals() {
            InvestorPortfolioResponse raw = portfolio(100000.0, 125000.0, 25000.0, 5000.0, 25.0, 12.5);

            InvestorPortfolioNewMobileResponse result = mapper.convert(raw);

            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getMsg()).isEqualTo("Success");
            assertThat(result.getCurrentCost()).isEqualTo(100000.0);
            assertThat(result.getCurrentValue()).isEqualTo(125000.0);
            assertThat(result.getUnrealisedGain()).isEqualTo(25000.0);
            assertThat(result.getRealisedGain()).isEqualTo(5000.0);
            assertThat(result.getAbsoluteReturn()).isEqualTo(25.0);
            assertThat(result.getXirr()).isEqualTo(12.5);
            assertThat(result.getPortfolioWeight()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("reports a zero weight when the portfolio has no value")
        void zeroValueMeansZeroWeight() {
            InvestorPortfolioNewMobileResponse result =
                    mapper.convert(portfolio(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));

            assertThat(result.getPortfolioWeight()).isZero();
        }

        @Test
        @DisplayName("survives a null portfolio rather than throwing")
        void handlesNullPortfolio() {
            InvestorPortfolioNewMobileResponse result = mapper.convert(null);

            assertThat(result.getStatus()).isEqualTo(200);
            assertThat(result.getCurrentValue()).isZero();
            assertThat(result.getList()).isEmpty();
        }

        @Test
        @DisplayName("survives null totals inside a present portfolio")
        void handlesNullTotals() {
            InvestorPortfolioResponse raw = new InvestorPortfolioResponse();
            raw.setTotalCurrentValue(null);
            raw.setTotalCurrentcost(null);
            raw.setTotalCAGR(null);

            InvestorPortfolioNewMobileResponse result = mapper.convert(raw);

            assertThat(result.getCurrentValue()).isZero();
            assertThat(result.getCurrentCost()).isZero();
            assertThat(result.getXirr()).isZero();
        }

        @Test
        @DisplayName("rounds monetary totals to two places")
        void roundsToTwoPlaces() {
            InvestorPortfolioResponse raw = portfolio(1.005, 2.348, 0.0, 0.0, 0.0, 3.14159);

            InvestorPortfolioNewMobileResponse result = mapper.convert(raw);

            assertThat(result.getCurrentValue()).isEqualTo(2.35);
            assertThat(result.getXirr()).isEqualTo(3.14);
        }
    }

    @Nested
    @DisplayName("category grouping")
    class Grouping {

        @Test
        @DisplayName("splits a colon-qualified category into its broad prefix")
        void splitsBroadCategory() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("HDFC Top 100", "Equity: Large Cap", 10000, 12000),
                    scheme("HDFC Mid Cap", "Equity: Mid Cap", 5000, 7000));

            List<BroadCategoryDto> broad = mapper.convert(raw).getList();

            assertThat(broad).hasSize(1);
            assertThat(broad.get(0).getBroadCategory()).isEqualTo("Equity");
            assertThat(broad.get(0).getCategoryList())
                    .extracting(CategoryDto::getCategory)
                    .containsExactly("Equity: Large Cap", "Equity: Mid Cap");
        }

        @Test
        @DisplayName("keeps separate broad categories apart, in encounter order")
        void keepsBroadCategoriesSeparate() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("HDFC Top 100", "Equity: Large Cap", 10000, 12000),
                    scheme("HDFC Liquid", "Debt: Liquid", 5000, 5200));

            assertThat(mapper.convert(raw).getList())
                    .extracting(BroadCategoryDto::getBroadCategory)
                    .containsExactly("Equity", "Debt");
        }

        @Test
        @DisplayName("uses a category without a colon as its own broad category")
        void categoryWithoutColon() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("Some Fund", "Hybrid", 1000, 1100));

            assertThat(mapper.convert(raw).getList().get(0).getBroadCategory()).isEqualTo("Hybrid");
        }

        @Test
        @DisplayName("falls back to scheme_class when no advisorkhoj category is set")
        void fallsBackToSchemeClass() {
            InvestorSchemeWisePortfolioResponse s = scheme("Fund", null, 1000, 1100);
            s.setScheme_class("Debt: Ultra Short");

            assertThat(mapper.convert(portfolioWith(s)).getList().get(0).getBroadCategory())
                    .isEqualTo("Debt");
        }

        @Test
        @DisplayName("falls back to Others when neither category is set")
        void fallsBackToOthers() {
            InvestorSchemeWisePortfolioResponse s = scheme("Fund", "   ", 1000, 1100);
            s.setScheme_class("");

            assertThat(mapper.convert(portfolioWith(s)).getList().get(0).getBroadCategory())
                    .isEqualTo("Others");
        }
    }

    @Nested
    @DisplayName("holding filtering")
    class Filtering {

        @Test
        @DisplayName("omits a fully redeemed holding from the scheme list")
        void omitsZeroValueHolding() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("Held", "Equity: Large Cap", 10000, 12000),
                    scheme("Redeemed", "Equity: Large Cap", 8000, 0));

            List<SchemeDto> schemes = mapper.convert(raw).getList()
                    .get(0).getCategoryList().get(0).getSchemeList();

            assertThat(schemes).extracting(SchemeDto::getSchemeAmfi).containsExactly("Held");
        }

        @Test
        @DisplayName("drops a category whose holdings are all fully redeemed")
        void dropsEmptyCategory() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("Held", "Equity: Large Cap", 10000, 12000),
                    scheme("Redeemed", "Debt: Liquid", 8000, 0));

            List<BroadCategoryDto> broad = mapper.convert(raw).getList();

            assertThat(broad).extracting(BroadCategoryDto::getBroadCategory)
                    .containsExactly("Equity", "Debt");
            assertThat(broad.get(1).getCategoryList())
                    .as("the Debt broad category survives but holds no categories")
                    .isEmpty();
        }

        /**
         * Category totals are summed over every holding in the category, including the ones the
         * scheme list filters out. A category can therefore report a cost with no visible holdings
         * behind it.
         */
        @Test
        @DisplayName("category totals still include the filtered-out holdings")
        void totalsIncludeFilteredHoldings() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("Held", "Equity: Large Cap", 10000, 12000),
                    scheme("Redeemed", "Equity: Large Cap", 8000, 0));

            CategoryDto category = mapper.convert(raw).getList().get(0).getCategoryList().get(0);

            assertThat(category.getSchemeList()).hasSize(1);
            assertThat(category.getCurrentCost()).isEqualTo(18000.0);
        }
    }

    @Nested
    @DisplayName("weights and returns")
    class WeightsAndReturns {

        @Test
        @DisplayName("apportions weight by share of total value")
        void apportionsWeight() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("A", "Equity: Large Cap", 10000, 30000),
                    scheme("B", "Debt: Liquid", 10000, 10000));
            raw.setTotalCurrentValue(40000.0);

            List<BroadCategoryDto> broad = mapper.convert(raw).getList();

            assertThat(broad.get(0).getPortfolioWeight()).isEqualTo(75.0);
            assertThat(broad.get(1).getPortfolioWeight()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("computes absolute return from cost and value")
        void computesAbsoluteReturn() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("A", "Equity: Large Cap", 10000, 12500));

            assertThat(mapper.convert(raw).getList().get(0).getAbsRtn()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("reports a zero return rather than dividing by a zero cost")
        void zeroCostYieldsZeroReturn() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("A", "Equity: Large Cap", 0, 5000));

            assertThat(mapper.convert(raw).getList().get(0).getAbsRtn()).isZero();
        }

        @Test
        @DisplayName("reports a zero scheme weight when the portfolio total is zero")
        void zeroTotalYieldsZeroSchemeWeight() {
            InvestorPortfolioResponse raw = portfolioWith(
                    scheme("A", "Equity: Large Cap", 1000, 1200));
            raw.setTotalCurrentValue(0.0);

            SchemeDto dto = mapper.convert(raw).getList()
                    .get(0).getCategoryList().get(0).getSchemeList().get(0);

            assertThat(dto.getSchemeWeight()).isZero();
        }
    }

    @Nested
    @DisplayName("scheme detail")
    class SchemeDetail {

        @Test
        @DisplayName("sums paid and reinvested dividend into one figure")
        void sumsDividend() {
            InvestorSchemeWisePortfolioResponse s = scheme("A", "Equity: Large Cap", 1000, 1200);
            s.setTotal_dividend_paid(150.0);
            s.setTotal_dividend_reinvest(75.5);

            assertThat(firstScheme(s).getDividend()).isEqualTo(225.5);
        }

        @Test
        @DisplayName("rounds units and NAV to four places")
        void roundsUnitsAndNav() {
            InvestorSchemeWisePortfolioResponse s = scheme("A", "Equity: Large Cap", 1000, 1200);
            s.setTotalUnits(123.456789);
            s.setLatestNav(45.987654);

            SchemeDto dto = firstScheme(s);

            assertThat(dto.getUnits()).isEqualTo(123.4568);
            assertThat(dto.getLatestNav()).isEqualTo(45.9877);
        }

        @Test
        @DisplayName("prefers a real date over its string form")
        void prefersRealDate() {
            InvestorSchemeWisePortfolioResponse s = scheme("A", "Equity: Large Cap", 1000, 1200);
            s.setInvestmentStartDate(date(2023, Calendar.APRIL, 15));
            s.setInvestmentStartDate_str("ignored");

            assertThat(firstScheme(s).getStartDate()).isEqualTo("15-04-2023");
        }

        @Test
        @DisplayName("falls back to the string date, then to empty")
        void fallsBackThroughDateForms() {
            InvestorSchemeWisePortfolioResponse withStr = scheme("A", "Equity: Large Cap", 1000, 1200);
            withStr.setInvestmentStartDate_str("01-01-2020");
            assertThat(firstScheme(withStr).getStartDate()).isEqualTo("01-01-2020");

            InvestorSchemeWisePortfolioResponse without = scheme("A", "Equity: Large Cap", 1000, 1200);
            assertThat(firstScheme(without).getStartDate()).isEmpty();
        }

        @Test
        @DisplayName("formats the NAV date the same way, with the same fallbacks")
        void formatsNavDate() {
            InvestorSchemeWisePortfolioResponse withDate = scheme("A", "Equity: Large Cap", 1000, 1200);
            withDate.setLatestNavDate(date(2024, Calendar.MARCH, 31));
            assertThat(firstScheme(withDate).getNavDate()).isEqualTo("31-03-2024");

            InvestorSchemeWisePortfolioResponse withStr = scheme("A", "Equity: Large Cap", 1000, 1200);
            withStr.setLatestNavDate_str("28-02-2024");
            assertThat(firstScheme(withStr).getNavDate()).isEqualTo("28-02-2024");

            InvestorSchemeWisePortfolioResponse without = scheme("A", "Equity: Large Cap", 1000, 1200);
            assertThat(firstScheme(without).getNavDate()).isEmpty();
        }

        @Test
        @DisplayName("defaults a missing investor name to empty rather than null")
        void defaultsInvestorName() {
            InvestorSchemeWisePortfolioResponse s = scheme("A", "Equity: Large Cap", 1000, 1200);
            s.setInvestorName(null);

            assertThat(firstScheme(s).getInvestorName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("logo resolution")
    class LogoResolution {

        @Test
        @DisplayName("prefers the canonical AMC name, which is reliably brand-first")
        void prefersSchemeCompany() {
            InvestorSchemeWisePortfolioResponse s = scheme("Top 100 Fund", "Equity: Large Cap", 1000, 1200);
            s.setScheme_company("HDFC Mutual Fund");

            assertThat(firstScheme(s).getLogo()).endsWith("hdfc.png");
        }

        @Test
        @DisplayName("falls through to the short name when the AMC name resolves to nothing")
        void fallsThroughToShortName() {
            InvestorSchemeWisePortfolioResponse s = scheme("Unbranded Fund", "Equity: Large Cap", 1000, 1200);
            s.setScheme_company("Unknown House");
            s.setScheme_amfi_short_name("Axis Bluechip");

            assertThat(firstScheme(s).getLogo()).endsWith("axis.png");
        }

        @Test
        @DisplayName("falls through to the raw scheme name last")
        void fallsThroughToSchemeName() {
            InvestorSchemeWisePortfolioResponse s = scheme("Kotak Emerging Equity", "Equity: Mid Cap", 1000, 1200);

            assertThat(firstScheme(s).getLogo()).endsWith("kotak.png");
        }

        @Test
        @DisplayName("returns the placeholder URL when nothing resolves")
        void returnsPlaceholder() {
            InvestorSchemeWisePortfolioResponse s = scheme("Nameless Fund", "Equity: Large Cap", 1000, 1200);

            assertThat(firstScheme(s).getLogo()).endsWith("empty.png");
        }

        @Test
        @DisplayName("skips blank candidates without treating them as a match")
        void skipsBlankCandidates() {
            InvestorSchemeWisePortfolioResponse s = scheme("Tata Digital India", "Equity: Sectoral", 1000, 1200);
            s.setScheme_company("   ");
            s.setScheme_amfi_short_name(null);

            assertThat(firstScheme(s).getLogo()).endsWith("tata.png");
        }
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private SchemeDto firstScheme(InvestorSchemeWisePortfolioResponse s) {
        return mapper.convert(portfolioWith(s)).getList()
                .get(0).getCategoryList().get(0).getSchemeList().get(0);
    }

    private static InvestorPortfolioResponse portfolio(
            double cost, double value, double unrealised, double realised, double absRtn, double cagr) {
        InvestorPortfolioResponse raw = new InvestorPortfolioResponse();
        raw.setTotalCurrentcost(cost);
        raw.setTotalCurrentValue(value);
        raw.setTotalUnReliasedGain(unrealised);
        raw.setTotalReliasedGain(realised);
        raw.setTotalAbsoluteReturn(absRtn);
        raw.setTotalCAGR(cagr);
        raw.setInvestorSchemeWisePortfolioResponses(new ArrayList<>());
        return raw;
    }

    private static InvestorPortfolioResponse portfolioWith(InvestorSchemeWisePortfolioResponse... schemes) {
        InvestorPortfolioResponse raw = new InvestorPortfolioResponse();
        List<InvestorSchemeWisePortfolioResponse> list = new ArrayList<>(List.of(schemes));
        double total = list.stream().mapToDouble(InvestorSchemeWisePortfolioResponse::getTotalCurrentValue).sum();
        raw.setTotalCurrentValue(total);
        raw.setInvestorSchemeWisePortfolioResponses(list);
        return raw;
    }

    private static InvestorSchemeWisePortfolioResponse scheme(
            String name, String category, double cost, double value) {
        InvestorSchemeWisePortfolioResponse s = new InvestorSchemeWisePortfolioResponse();
        s.setScheme(name);
        s.setScheme_advisorkhoj_category(category);
        s.setCurrentCostOfInvestment(cost);
        s.setTotalCurrentValue(value);
        s.setInvestorName("Asha Menon");
        s.setFoliono("123456/78");
        return s;
    }

    private static Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day);
        return calendar.getTime();
    }
}
