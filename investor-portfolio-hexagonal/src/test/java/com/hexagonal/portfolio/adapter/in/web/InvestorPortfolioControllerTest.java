package com.hexagonal.portfolio.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hexagonal.portfolio.application.port.in.ConvertToCapitalGainReportUseCase;
import com.hexagonal.portfolio.application.port.in.ConvertToMobilePortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetFamilyMfPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetFolioMasterSummaryUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorTaxReportUseCase;
import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse;
import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse;
import com.hexagonal.portfolio.domain.model.FolioMasterSummaryResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;

/**
 * Adapter tests for {@link InvestorPortfolioController}.
 *
 * <p>The controller depends only on the driving ports, so it is exercised in complete
 * isolation — no Spring context, no datasource, no AMFI schema. That is the practical
 * payoff of the hexagonal split, and it lets these tests assert the two things the web
 * adapter is actually responsible for:
 *
 * <ol>
 *   <li>which use case each combination of {@code report_type} / {@code source} selects, and</li>
 *   <li>the exact arguments handed to it after {@code checkParem} normalisation.</li>
 * </ol>
 *
 * <p>The valuation maths itself is deliberately out of scope here — it belongs to the
 * application layer and is stubbed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvestorPortfolioControllerTest {

    @Mock
    private GetInvestorPortfolioUseCase getInvestorPortfolioUseCase;
    @Mock
    private GetFamilyMfPortfolioUseCase getFamilyMfPortfolioUseCase;
    @Mock
    private GetFolioMasterSummaryUseCase getFolioMasterSummaryUseCase;
    @Mock
    private ConvertToMobilePortfolioUseCase convertToMobilePortfolioUseCase;
    @Mock
    private GetInvestorTaxReportUseCase getInvestorTaxReportUseCase;
    @Mock
    private ConvertToCapitalGainReportUseCase convertToCapitalGainReportUseCase;

    @InjectMocks
    private InvestorPortfolioController controller;

    private MockMvc mockMvc;

    private static final String URL = "/investor/getInvestorPortfolioNew";
    private static final String TAX_URL = "/investor/getTaxReportsNew";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =====================================================================
    // Default branch — no report_type, no source
    // =====================================================================

    @Nested
    @DisplayName("default branch (no report_type, no source)")
    class DefaultBranch {

        @Test
        @DisplayName("returns the portfolio and delegates the raw normalised parameters")
        void returnsPortfolio() throws Exception {
            InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
            portfolio.setId(42);
            portfolio.setInvestorName("Asha Menon");
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(portfolio);

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("financial_year", "2023-2024")
                            .param("folio_type", "Live")
                            .param("selected_date", "31-03-2024")
                            .param("summary_type", "holding-report"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.investorName").value("Asha Menon"));

            verify(getInvestorPortfolioUseCase).getInvestorPortfolioNew(
                    42, "2023-2024", "Live", "31-03-2024", "acme", "holding-report");
            verifyNoInteractions(getFamilyMfPortfolioUseCase, getFolioMasterSummaryUseCase,
                    convertToMobilePortfolioUseCase);
        }

        /**
         * Only the family branch substitutes "All" for blank inputs. The default branch
         * forwards the blanks untouched, and that difference is load-bearing.
         */
        @Test
        @DisplayName("forwards blank optional parameters as empty strings, not \"All\"")
        void forwardsBlanksVerbatim() throws Exception {
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new InvestorPortfolioResponse());

            mockMvc.perform(get(URL).param("userId", "7").param("clientName", "acme"))
                    .andExpect(status().isOk());

            verify(getInvestorPortfolioUseCase).getInvestorPortfolioNew(7, "", "", "", "acme", "");
        }
    }

    // =====================================================================
    // checkParem normalisation
    // =====================================================================

    @Nested
    @DisplayName("checkParem normalisation")
    class Normalisation {

        @ParameterizedTest(name = "\"{0}\" is normalised to an empty string")
        @ValueSource(strings = {"null", "NULL", "undefined", "UNDEFINED", "   "})
        void normalisesPlaceholderValues(String raw) throws Exception {
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new InvestorPortfolioResponse());

            mockMvc.perform(get(URL)
                            .param("userId", "7")
                            .param("clientName", "acme")
                            .param("financial_year", raw)
                            .param("folio_type", raw))
                    .andExpect(status().isOk());

            verify(getInvestorPortfolioUseCase).getInvestorPortfolioNew(7, "", "", "", "acme", "");
        }

        @Test
        @DisplayName("trims surrounding whitespace from real values")
        void trimsRealValues() throws Exception {
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new InvestorPortfolioResponse());

            mockMvc.perform(get(URL)
                            .param("userId", "  7  ")
                            .param("clientName", "  acme  ")
                            .param("folio_type", "  Live  "))
                    .andExpect(status().isOk());

            verify(getInvestorPortfolioUseCase).getInvestorPortfolioNew(7, "", "Live", "", "acme", "");
        }
    }

    // =====================================================================
    // report_type=family
    // =====================================================================

    @Nested
    @DisplayName("report_type=family")
    class FamilyBranch {

        @Test
        @DisplayName("substitutes \"All\" for blank year, folio type and summary type")
        void defaultsBlanksToAll() throws Exception {
            when(getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(familyResponse());

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("report_type", "family"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.status_msg").value("Success"));

            verify(getFamilyMfPortfolioUseCase).getFamilyPortfolio(42, "All", "All", "", "acme", "All");
            verifyNoInteractions(getInvestorPortfolioUseCase, getFolioMasterSummaryUseCase,
                    convertToMobilePortfolioUseCase);
        }

        @Test
        @DisplayName("passes explicit values through untouched")
        void passesExplicitValues() throws Exception {
            when(getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(familyResponse());

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("report_type", "family")
                            .param("financial_year", "2023-2024")
                            .param("folio_type", "Live")
                            .param("selected_date", "31-03-2024")
                            .param("summary_type", "holding-report"))
                    .andExpect(status().isOk());

            verify(getFamilyMfPortfolioUseCase).getFamilyPortfolio(
                    42, "2023-2024", "Live", "31-03-2024", "acme", "holding-report");
        }

        @Test
        @DisplayName("wins over source=mobile, because the family check runs first")
        void takesPrecedenceOverMobile() throws Exception {
            when(getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(familyResponse());

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("report_type", "family")
                            .param("source", "mobile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status_msg").value("Success"));

            verifyNoInteractions(convertToMobilePortfolioUseCase);
        }
    }

    // =====================================================================
    // report_type=folio
    // =====================================================================

    @Nested
    @DisplayName("report_type=folio")
    class FolioBranch {

        @Test
        @DisplayName("feeds the computed portfolio into the folio master summary")
        void buildsFolioSummaryFromPortfolio() throws Exception {
            InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
            portfolio.setId(42);
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(portfolio);

            FolioMasterSummaryResponse folio = new FolioMasterSummaryResponse();
            folio.setStatus(200);
            folio.setStatus_msg("Success");
            folio.setTotal_current_value(150000.0);
            when(getFolioMasterSummaryUseCase.buildFolioMasterSummary(any(), anyInt(), anyString()))
                    .thenReturn(folio);

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("report_type", "folio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.total_current_value").value(150000.0));

            // the very portfolio instance just computed must be the one summarised
            verify(getFolioMasterSummaryUseCase).buildFolioMasterSummary(eq(portfolio), eq(42), eq("acme"));
            verifyNoInteractions(getFamilyMfPortfolioUseCase, convertToMobilePortfolioUseCase);
        }
    }

    // =====================================================================
    // source=mobile
    // =====================================================================

    @Nested
    @DisplayName("source=mobile")
    class MobileBranch {

        @Test
        @DisplayName("converts the portfolio into the mobile payload")
        void convertsToMobilePayload() throws Exception {
            InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(portfolio);

            InvestorPortfolioNewMobileResponse mobile = new InvestorPortfolioNewMobileResponse();
            mobile.setStatus(200);
            mobile.setMsg("Success");
            mobile.setCurrentValue(250000.0);
            when(convertToMobilePortfolioUseCase.convert(any())).thenReturn(mobile);

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("source", "mobile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.current_value").value(250000.0));

            verify(convertToMobilePortfolioUseCase).convert(portfolio);
        }

        @Test
        @DisplayName("a non-mobile source falls through to the plain portfolio")
        void otherSourceReturnsPortfolio() throws Exception {
            InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
            portfolio.setId(42);
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(portfolio);

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("source", "web"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(42));

            verify(convertToMobilePortfolioUseCase, never()).convert(any());
        }
    }

    // =====================================================================
    // Branch selection is case-insensitive
    // =====================================================================

    @ParameterizedTest(name = "report_type=\"{0}\" routes to the family use case")
    @ValueSource(strings = {"family", "FAMILY", "Family", "FaMiLy"})
    @DisplayName("report_type matching ignores case")
    void reportTypeIsCaseInsensitive(String reportType) throws Exception {
        when(getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(familyResponse());

        mockMvc.perform(get(URL)
                        .param("userId", "42")
                        .param("clientName", "acme")
                        .param("report_type", reportType))
                .andExpect(status().isOk());

        verify(getFamilyMfPortfolioUseCase).getFamilyPortfolio(
                anyInt(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "source=\"{0}\" routes to the mobile mapper")
    @ValueSource(strings = {"mobile", "MOBILE", "Mobile"})
    @DisplayName("source matching ignores case")
    void sourceIsCaseInsensitive(String source) throws Exception {
        when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new InvestorPortfolioResponse());
        when(convertToMobilePortfolioUseCase.convert(any()))
                .thenReturn(new InvestorPortfolioNewMobileResponse());

        mockMvc.perform(get(URL)
                        .param("userId", "42")
                        .param("clientName", "acme")
                        .param("source", source))
                .andExpect(status().isOk());

        verify(convertToMobilePortfolioUseCase).convert(any());
    }

    // =====================================================================
    // Failure handling — bad input is a 400, a server fault is a 500
    // =====================================================================

    /**
     * The endpoint used to answer every failure with an empty-bodied 400, so a null dereference in
     * the valuation was indistinguishable from a mistyped investor id. These tests pin the split
     * that {@link GlobalExceptionHandler} restores: the caller's fault is a 4xx, ours is a 5xx.
     */
    @Nested
    @DisplayName("failure handling")
    class Failures {

        @Test
        @DisplayName("a missing userId yields 400 with the invalid-parameter code")
        void missingUserIdIsBadRequest() throws Exception {
            mockMvc.perform(get(URL).param("clientName", "acme"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PORTFOLIO-4001"));

            verifyNoInteractions(getInvestorPortfolioUseCase);
        }

        @ParameterizedTest(name = "userId=\"{0}\" yields 400")
        @ValueSource(strings = {"abc", "4.2", "null", "undefined", ""})
        @DisplayName("a non-numeric userId yields 400 without reaching the use case")
        void nonNumericUserIdIsBadRequest(String userId) throws Exception {
            mockMvc.perform(get(URL).param("userId", userId).param("clientName", "acme"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PORTFOLIO-4001"));

            verifyNoInteractions(getInvestorPortfolioUseCase);
        }

        @Test
        @DisplayName("an exception from the use case is a 500, no longer disguised as a 400")
        void useCaseFailureIsInternalServerError() throws Exception {
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("valuation blew up"));

            mockMvc.perform(get(URL).param("userId", "42").param("clientName", "acme"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("PORTFOLIO-5000"));
        }

        @Test
        @DisplayName("a failure inside the family branch is also a 500")
        void familyFailureIsInternalServerError() throws Exception {
            when(getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new IllegalStateException("no family mapping"));

            mockMvc.perform(get(URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("report_type", "family"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("PORTFOLIO-5000"));
        }

        @Test
        @DisplayName("the 500 body carries no stack trace, exception class or internal message")
        void internalErrorLeaksNothing() throws Exception {
            when(getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new IllegalStateException("connection pool exhausted at 10.0.0.7"));

            String body = mockMvc.perform(get(URL).param("userId", "42").param("clientName", "acme"))
                    .andExpect(status().isInternalServerError())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .doesNotContain("connection pool exhausted")
                    .doesNotContain("IllegalStateException")
                    .doesNotContain("com.hexagonal");
        }
    }

    // =====================================================================
    // getTaxReportsNew — same controller, so covered here too
    // =====================================================================

    @Nested
    @DisplayName("getTaxReportsNew")
    class TaxReports {

        @Test
        @DisplayName("returns the raw rows when source is not mobile")
        void returnsRawRows() throws Exception {
            when(getInvestorTaxReportUseCase.getInvestorTaxReportNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of(new InvestorSchemeWiseTransactionTaxReport()));

            mockMvc.perform(get(TAX_URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("option", "equity")
                            .param("source", "web"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(getInvestorTaxReportUseCase).getInvestorTaxReportNew(
                    42, "acme", "equity", "All", "", "");
            verifyNoInteractions(convertToCapitalGainReportUseCase);
        }

        @Test
        @DisplayName("applies the declared defaults for financialYear, startDate and endDate")
        void appliesDeclaredDefaults() throws Exception {
            when(getInvestorTaxReportUseCase.getInvestorTaxReportNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get(TAX_URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("option", "debt")
                            .param("source", "web"))
                    .andExpect(status().isOk());

            verify(getInvestorTaxReportUseCase).getInvestorTaxReportNew(
                    42, "acme", "debt", "All", "", "");
        }

        @Test
        @DisplayName("passes an explicit date range straight through")
        void passesExplicitDateRange() throws Exception {
            when(getInvestorTaxReportUseCase.getInvestorTaxReportNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(get(TAX_URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("option", "equity")
                            .param("financialYear", "2023-2024")
                            .param("startDate", "01-04-2023")
                            .param("endDate", "31-03-2024")
                            .param("source", "web"))
                    .andExpect(status().isOk());

            verify(getInvestorTaxReportUseCase).getInvestorTaxReportNew(
                    42, "acme", "equity", "2023-2024", "01-04-2023", "31-03-2024");
        }

        @ParameterizedTest(name = "source=\"{0}\" returns the capital-gain payload")
        @CsvSource({"mobile", "MOBILE", "Mobile"})
        @DisplayName("converts to the capital-gain payload for mobile, ignoring case")
        void convertsForMobile(String source) throws Exception {
            List<InvestorSchemeWiseTransactionTaxReport> rows =
                    List.of(new InvestorSchemeWiseTransactionTaxReport());
            when(getInvestorTaxReportUseCase.getInvestorTaxReportNew(
                    anyInt(), anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(rows);

            CapitalGainReportApiResponse converted = new CapitalGainReportApiResponse();
            converted.setStatus(200);
            converted.setMsg("Success");
            when(convertToCapitalGainReportUseCase.convert(any())).thenReturn(converted);

            mockMvc.perform(get(TAX_URL)
                            .param("userId", "42")
                            .param("clientName", "acme")
                            .param("option", "equity")
                            .param("source", source))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.msg").value("Success"));

            verify(convertToCapitalGainReportUseCase).convert(rows);
        }

        @Test
        @DisplayName("a missing required parameter is rejected with 400")
        void missingRequiredParamIsBadRequest() throws Exception {
            mockMvc.perform(get(TAX_URL).param("userId", "42").param("clientName", "acme"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(getInvestorTaxReportUseCase);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static FamilyMfPortfolioResponse familyResponse() {
        FamilyMfPortfolioResponse response = new FamilyMfPortfolioResponse();
        response.setStatus(200);
        response.setStatusMsg("Success");
        response.setMsg("Success");
        return response;
    }
}
