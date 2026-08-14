package com.hexagonal.portfolio.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.hexagonal.portfolio.application.port.out.LoadCamsFolioMasterPort;
import com.hexagonal.portfolio.application.port.out.LoadKarvyFolioMasterPort;
import com.hexagonal.portfolio.domain.model.FolioMasterResponse;
import com.hexagonal.portfolio.domain.model.FolioMasterSummary;
import com.hexagonal.portfolio.domain.model.FolioMasterSummaryResponse;
import com.hexagonal.portfolio.domain.model.InvestorMasterCams;
import com.hexagonal.portfolio.domain.model.InvestorMasterKarvy;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWisePortfolioResponse;

/**
 * Tests for the folio master summary (`report_type=folio`).
 *
 * <p>The behaviour that carries risk here is the join: each holding is matched to its registrar
 * master rows on folio number *and* product code together. Loosening that to folio alone would
 * attach one scheme's bank and nominee details to another scheme in the same folio.
 */
@DisplayName("FolioMasterSummaryService")
class FolioMasterSummaryServiceTest {

    private static final String LOGO_BASE = "https://cdn.example.com/logos/";

    private LoadCamsFolioMasterPort camsPort;
    private LoadKarvyFolioMasterPort karvyPort;
    private FolioMasterSummaryService service;

    @BeforeEach
    void setUp() {
        camsPort = mock(LoadCamsFolioMasterPort.class);
        karvyPort = mock(LoadKarvyFolioMasterPort.class);
        service = new FolioMasterSummaryService(camsPort, karvyPort, LOGO_BASE);
    }

    @Nested
    @DisplayName("buildFolioMasterSummary")
    class BuildSummary {

        @Test
        @DisplayName("returns a zeroed success response for a null portfolio")
        void nullPortfolioIsZeroed() {
            FolioMasterSummaryResponse response = service.buildFolioMasterSummary(null, 1, "acme");

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getStatus_msg()).isEqualTo("Success");
            assertThat(response.getTotal_current_value()).isZero();
            assertThat(response.getFolio_master_summary()).isEmpty();
        }

        @Test
        @DisplayName("returns an empty summary when the portfolio holds nothing")
        void noHoldingsYieldsEmptySummary() {
            InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
            portfolio.setTotalCurrentValue(0.0);
            portfolio.setInvestorSchemeWisePortfolioResponses(new ArrayList<>());

            FolioMasterSummaryResponse response = service.buildFolioMasterSummary(portfolio, 1, "acme");

            assertThat(response.getFolio_master_summary()).isEmpty();
            assertThat(response.getTotal_current_value()).isZero();
        }

        @Test
        @DisplayName("carries the portfolio total across unchanged")
        void carriesPortfolioTotal() {
            InvestorPortfolioResponse portfolio = portfolioWith(150000.0, holding("HDFC Top 100", "F1", "P1"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of());
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.buildFolioMasterSummary(portfolio, 1, "acme").getTotal_current_value())
                    .isEqualTo(150000.0);
        }

        @Test
        @DisplayName("emits one summary row per holding, with a resolved logo URL")
        void oneRowPerHolding() {
            InvestorPortfolioResponse portfolio = portfolioWith(1000.0,
                    holding("HDFC Top 100", "F1", "P1"),
                    holding("Axis Bluechip", "F2", "P2"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of());
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            List<FolioMasterSummary> summary =
                    service.buildFolioMasterSummary(portfolio, 1, "acme").getFolio_master_summary();

            assertThat(summary).extracting(FolioMasterSummary::getScheme)
                    .containsExactly("HDFC Top 100", "Axis Bluechip");
            assertThat(summary.get(0).getAmc_logo()).isEqualTo(LOGO_BASE + "hdfc.png");
            assertThat(summary.get(1).getAmc_logo()).isEqualTo(LOGO_BASE + "axis.png");
            assertThat(summary.get(0).getFolio_no()).isEqualTo("F1");
        }

        /**
         * The join is on folio number AND product code. A folio can hold several schemes, so
         * matching on the folio alone would attach the wrong scheme's registrar details.
         */
        @Test
        @DisplayName("matches master rows on folio number and product code together")
        void matchesOnFolioAndProduct() {
            InvestorPortfolioResponse portfolio = portfolioWith(1000.0, holding("Scheme A", "F1", "P1"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(
                    cams("F1", "P1", "Scheme A", "Single"),
                    cams("F1", "P2", "Scheme B", "Joint")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            FolioMasterSummary row =
                    service.buildFolioMasterSummary(portfolio, 1, "acme").getFolio_master_summary().get(0);

            assertThat(row.getFolio_master_details()).hasSize(1);
            assertThat(row.getFolio_master_details().get(0).getProduct_code()).isEqualTo("P1");
            assertThat(row.getMod_of_holding())
                    .as("must come from the matched row, not the other scheme in the same folio")
                    .isEqualTo("Single");
        }

        @Test
        @DisplayName("matches case-insensitively on both keys")
        void matchIsCaseInsensitive() {
            InvestorPortfolioResponse portfolio = portfolioWith(1000.0, holding("Scheme A", "f1", "p1"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(cams("F1", "P1", "Scheme A", "Single")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.buildFolioMasterSummary(portfolio, 1, "acme")
                    .getFolio_master_summary().get(0).getFolio_master_details())
                    .hasSize(1);
        }

        @Test
        @DisplayName("leaves details empty when nothing matches, rather than failing")
        void noMatchYieldsEmptyDetails() {
            InvestorPortfolioResponse portfolio = portfolioWith(1000.0, holding("Scheme A", "F9", "P9"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(cams("F1", "P1", "Scheme A", "Single")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            FolioMasterSummary row =
                    service.buildFolioMasterSummary(portfolio, 1, "acme").getFolio_master_summary().get(0);

            assertThat(row.getFolio_master_details()).isEmpty();
            assertThat(row.getMod_of_holding()).isEmpty();
        }

        /**
         * `holding_nature` is never populated on the portfolio scheme, so the mode of holding is
         * derived from the first matched master row that actually carries one.
         */
        @Test
        @DisplayName("takes the first non-blank mode of holding from the matched rows")
        void skipsBlankModeOfHolding() {
            InvestorPortfolioResponse portfolio = portfolioWith(1000.0, holding("Scheme A", "F1", "P1"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(
                    cams("F1", "P1", "Scheme A", "   "),
                    cams("F1", "P1", "Scheme A", "Anyone or Survivor")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.buildFolioMasterSummary(portfolio, 1, "acme")
                    .getFolio_master_summary().get(0).getMod_of_holding())
                    .isEqualTo("Anyone or Survivor");
        }

        @Test
        @DisplayName("ignores master rows with a null folio or product code")
        void ignoresNullKeys() {
            InvestorPortfolioResponse portfolio = portfolioWith(1000.0, holding("Scheme A", "F1", "P1"));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(
                    cams(null, "P1", "Scheme A", "Single"),
                    cams("F1", null, "Scheme A", "Single")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.buildFolioMasterSummary(portfolio, 1, "acme")
                    .getFolio_master_summary().get(0).getFolio_master_details())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("getSchemeMaster")
    class SchemeMaster {

        @Test
        @DisplayName("combines CAMS and Karvy rows into one list")
        void combinesBothRegistrars() {
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(cams("F1", "P1", "Cams Scheme", "Single")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString()))
                    .thenReturn(List.of(karvy("F2", "P2", "Karvy Scheme")));

            List<FolioMasterResponse> result = service.getSchemeMaster(1, "acme");

            assertThat(result).extracting(FolioMasterResponse::getFolio_number)
                    .containsExactly("F1", "F2");
            assertThat(result).extracting(FolioMasterResponse::getScheme)
                    .containsExactly("Cams Scheme", "Karvy Scheme");
        }

        @Test
        @DisplayName("joins the three address lines with commas")
        void joinsAddressLines() {
            InvestorMasterCams row = cams("F1", "P1", "Scheme", "Single");
            row.setAddress1("12 Main St");
            row.setAddress2("Indiranagar");
            row.setAddress3("Bengaluru");
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(row));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.getSchemeMaster(1, "acme").get(0).getAddress())
                    .isEqualTo("12 Main St,Indiranagar,Bengaluru");
        }

        @Test
        @DisplayName("renders a missing address line as empty rather than the text null")
        void nullAddressLineIsEmpty() {
            InvestorMasterCams row = cams("F1", "P1", "Scheme", "Single");
            row.setAddress1("12 Main St");
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(row));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.getSchemeMaster(1, "acme").get(0).getAddress())
                    .isEqualTo("12 Main St,,")
                    .doesNotContain("null");
        }

        @Test
        @DisplayName("formats dates as dd-MM-yyyy and a missing date as empty")
        void formatsDates() {
            InvestorMasterCams withDate = cams("F1", "P1", "Scheme", "Single");
            withDate.setInv_dob(date(1985, Calendar.JUNE, 9));
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of(withDate, cams("F2", "P2", "S", "Single")));
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            List<FolioMasterResponse> result = service.getSchemeMaster(1, "acme");

            assertThat(result.get(0).getDob()).isEqualTo("09-06-1985");
            assertThat(result.get(1).getDob()).isEmpty();
        }

        @Test
        @DisplayName("returns an empty list when the investor holds nothing at either registrar")
        void noRowsYieldsEmptyList() {
            when(camsPort.findData(anyInt(), anyString())).thenReturn(List.of());
            when(karvyPort.findByUserIdAndClientNameOrderByIdDesc(anyInt(), anyString())).thenReturn(List.of());

            assertThat(service.getSchemeMaster(1, "acme")).isEmpty();
        }

        /**
         * Unlike the valuation, this path does not swallow its failure — it wraps and rethrows, so
         * a registrar read error surfaces as a 500 rather than as a silently short folio list.
         */
        @Test
        @DisplayName("wraps and rethrows a registrar failure rather than returning partial data")
        void registrarFailurePropagates() {
            when(camsPort.findData(anyInt(), anyString()))
                    .thenThrow(new IllegalStateException("cams read failed"));

            assertThatThrownBy(() -> service.getSchemeMaster(1, "acme"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to getSchemeMaster")
                    .hasRootCauseMessage("cams read failed");
        }
    }

    // ---------------------------------------------------------------------

    private static InvestorPortfolioResponse portfolioWith(
            double total, InvestorSchemeWisePortfolioResponse... holdings) {
        InvestorPortfolioResponse portfolio = new InvestorPortfolioResponse();
        portfolio.setTotalCurrentValue(total);
        portfolio.setInvestorSchemeWisePortfolioResponses(new ArrayList<>(List.of(holdings)));
        return portfolio;
    }

    private static InvestorSchemeWisePortfolioResponse holding(String scheme, String folio, String schemeCode) {
        InvestorSchemeWisePortfolioResponse h = new InvestorSchemeWisePortfolioResponse();
        h.setScheme(scheme);
        h.setFoliono(folio);
        h.setScheme_code(schemeCode);
        h.setTotalUnits(100.0);
        h.setTotalCurrentValue(1000.0);
        return h;
    }

    private static InvestorMasterCams cams(String folio, String product, String scheme, String holdingNature) {
        InvestorMasterCams row = new InvestorMasterCams();
        row.setFoliochk(folio);
        row.setProduct(product);
        row.setSch_name(scheme);
        row.setHolding_na(holdingNature);
        return row;
    }

    private static InvestorMasterKarvy karvy(String folio, String product, String scheme) {
        InvestorMasterKarvy row = new InvestorMasterKarvy();
        row.setFolio(folio);
        row.setProduct_code(product);
        row.setFund_description(scheme);
        return row;
    }

    private static Date date(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day);
        return calendar.getTime();
    }
}
