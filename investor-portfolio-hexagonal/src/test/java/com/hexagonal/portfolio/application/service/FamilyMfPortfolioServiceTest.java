package com.hexagonal.portfolio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hexagonal.portfolio.application.port.in.GetInvestorPortfolioUseCase;
import com.hexagonal.portfolio.application.port.out.LoadFamilyMappingPort;
import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse;
import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse.MemberMfSummary;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.UsersMapping;

/**
 * Tests for the family roll-up (`report_type=family`).
 *
 * <p>This service fans out to the single-investor use case once per mapped member and sums the
 * results, so the risks are in the fan-out rather than the arithmetic: a member silently dropped,
 * a member counted twice, or the head/member classification inverted.
 */
@DisplayName("FamilyMfPortfolioService")
class FamilyMfPortfolioServiceTest {

    private LoadFamilyMappingPort familyMappingPort;
    private GetInvestorPortfolioUseCase portfolioUseCase;
    private FamilyMfPortfolioService service;

    @BeforeEach
    void setUp() {
        familyMappingPort = mock(LoadFamilyMappingPort.class);
        portfolioUseCase = mock(GetInvestorPortfolioUseCase.class);
        service = new FamilyMfPortfolioService(familyMappingPort, portfolioUseCase);
    }

    @Nested
    @DisplayName("membership")
    class Membership {

        @Test
        @DisplayName("returns an empty success response when the family has no members")
        void noMembers() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString())).thenReturn(List.of());

            FamilyMfPortfolioResponse response =
                    service.getFamilyPortfolio(1, "All", "All", "", "acme", "All");

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getMsg()).isEqualTo("Success");
            assertThat(response.getMfSchemeSummary()).isEmpty();
            assertThat(response.getSipSchemeSummary()).isEmpty();
        }

        /**
         * A null mapping result is treated as an empty family rather than propagating — the
         * endpoint answers 200 with nothing in it instead of failing.
         */
        @Test
        @DisplayName("treats a null mapping result as an empty family")
        void nullMembers() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString())).thenReturn(null);

            assertThat(service.getFamilyPortfolio(1, "All", "All", "", "acme", "All").getMfSchemeSummary())
                    .isEmpty();
        }

        @Test
        @DisplayName("emits one block per member, in mapping order")
        void oneBlockPerMember() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha Menon", "Asha Menon"), member(2, "Ravi Menon", "Asha Menon")));
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(10000.0, 12000.0));

            List<MemberMfSummary> summary =
                    service.getFamilyPortfolio(1, "All", "All", "", "acme", "All").getMfSchemeSummary();

            assertThat(summary).extracting(MemberMfSummary::getInvestorName)
                    .containsExactly("Asha Menon", "Ravi Menon");
        }

        /**
         * A member whose portfolio comes back null is skipped entirely. The family total is then
         * quietly short by that member's holdings — worth knowing when a roll-up looks low.
         */
        @Test
        @DisplayName("skips a member whose portfolio is null, and omits them from the total")
        void skipsNullPortfolio() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha", "Asha"), member(2, "Ravi", "Asha")));
            when(portfolioUseCase.getInvestorPortfolioNew(eq(1), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(10000.0, 12000.0));
            when(portfolioUseCase.getInvestorPortfolioNew(eq(2), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(null);

            FamilyMfPortfolioResponse response =
                    service.getFamilyPortfolio(1, "All", "All", "", "acme", "All");

            assertThat(response.getMfSchemeSummary()).hasSize(1);
            assertThat(response.getMfSummary().getTotalCurrValue()).isEqualTo(12000.0);
        }

        @Test
        @DisplayName("classifies the member whose mapping name matches their own as the family head")
        void classifiesFamilyHead() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha Menon", "Asha Menon"), member(2, "Ravi Menon", "Asha Menon")));
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(1000.0, 1100.0));

            assertThat(service.getFamilyPortfolio(1, "All", "All", "", "acme", "All").getMfSchemeSummary())
                    .extracting(MemberMfSummary::getFamilyStatus)
                    .containsExactly("Family Head", "Family Member");
        }

        @Test
        @DisplayName("matches the head regardless of case")
        void headMatchIsCaseInsensitive() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "asha menon", "ASHA MENON")));
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(1000.0, 1100.0));

            assertThat(service.getFamilyPortfolio(1, "All", "All", "", "acme", "All")
                    .getMfSchemeSummary().get(0).getFamilyStatus())
                    .isEqualTo("Family Head");
        }
    }

    @Nested
    @DisplayName("roll-up")
    class RollUp {

        @Test
        @DisplayName("sums cost, value and gains across members")
        void sumsAcrossMembers() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha", "Asha"), member(2, "Ravi", "Asha")));
            when(portfolioUseCase.getInvestorPortfolioNew(eq(1), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(10000.0, 12000.0));
            when(portfolioUseCase.getInvestorPortfolioNew(eq(2), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(5000.0, 5500.0));

            var summary = service.getFamilyPortfolio(1, "All", "All", "", "acme", "All").getMfSummary();

            assertThat(summary.getTotalCurrCost()).isEqualTo(15000.0);
            assertThat(summary.getTotalCurrValue()).isEqualTo(17500.0);
        }

        @Test
        @DisplayName("computes each member's absolute return from their own cost")
        void perMemberAbsoluteReturn() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha", "Asha")));
            InvestorPortfolioResponse p = portfolio(10000.0, 12500.0);
            p.setTotalUnReliasedGain(2500.0);
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(p);

            assertThat(service.getFamilyPortfolio(1, "All", "All", "", "acme", "All")
                    .getMfSchemeSummary().get(0).getAbsRtn())
                    .isCloseTo(25.0, within(0.01));
        }

        @Test
        @DisplayName("reports a zero return rather than dividing by a zero cost")
        void zeroCostYieldsZeroReturn() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha", "Asha")));
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(0.0, 0.0));

            var response = service.getFamilyPortfolio(1, "All", "All", "", "acme", "All");

            assertThat(response.getMfSchemeSummary().get(0).getAbsRtn()).isZero();
            assertThat(response.getMfSummary().getTotalAbsRtn()).isZero();
        }

        @Test
        @DisplayName("treats null totals on a member portfolio as zero")
        void nullTotalsAreZero() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(1, "Asha", "Asha")));
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(new InvestorPortfolioResponse());

            var response = service.getFamilyPortfolio(1, "All", "All", "", "acme", "All");

            assertThat(response.getMfSummary().getTotalCurrValue()).isZero();
            assertThat(response.getMfSchemeSummary().get(0).getCurrentValue()).isZero();
        }
    }

    @Nested
    @DisplayName("delegation")
    class Delegation {

        @Test
        @DisplayName("passes each member's id and the shared filters to the portfolio use case")
        void forwardsFiltersPerMember() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString()))
                    .thenReturn(List.of(member(11, "Asha", "Asha"), member(22, "Ravi", "Asha")));
            when(portfolioUseCase.getInvestorPortfolioNew(anyInt(), anyString(), anyString(), anyString(),
                    anyString(), anyString())).thenReturn(portfolio(1.0, 1.0));

            service.getFamilyPortfolio(1, "2023-2024", "Live", "31-03-2024", "acme", "holding-report");

            verify(portfolioUseCase).getInvestorPortfolioNew(
                    11, "2023-2024", "Live", "31-03-2024", "acme", "holding-report");
            verify(portfolioUseCase).getInvestorPortfolioNew(
                    22, "2023-2024", "Live", "31-03-2024", "acme", "holding-report");
        }

        @Test
        @DisplayName("looks the family up by head id and client name")
        void looksUpFamilyByHead() {
            when(familyMappingPort.getFamilyMappedInvestor(anyInt(), anyString())).thenReturn(List.of());

            service.getFamilyPortfolio(77, "All", "All", "", "acme", "All");

            verify(familyMappingPort).getFamilyMappedInvestor(77, "acme");
        }
    }

    // ---------------------------------------------------------------------

    private static UsersMapping member(Integer investorId, String investorName, String mappingName) {
        UsersMapping mapping = new UsersMapping();
        mapping.setInvestor_id(investorId);
        mapping.setInvestor_name(investorName);
        mapping.setMapping_name(mappingName);
        mapping.setPan("ABCDE1234F");
        return mapping;
    }

    private static InvestorPortfolioResponse portfolio(double cost, double value) {
        InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
        portfolio.setTotalCurrentcost(cost);
        portfolio.setTotalCurrentValue(value);
        portfolio.setInvestorSchemeWisePortfolioResponses(new ArrayList<>());
        return portfolio;
    }
}
