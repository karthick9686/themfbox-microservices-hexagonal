package com.hexagonal.portfolio.adapter.in.web;

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
import com.hexagonal.portfolio.domain.service.MfboxUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Driving adapter for the investor-portfolio read model.
 *
 * <p>Mirrors the legacy {@code /investor/getInvestorPortfolioNew} endpoint exactly: same path,
 * same optional request parameters, same {@code checkParem} normalisation, same
 * {@code report_type} / {@code source} branching, and the same bad-request fallback so the
 * emitted payloads are byte-for-byte equivalent.
 */
@RestController
@RequestMapping("/investor")
@RequiredArgsConstructor
public class InvestorPortfolioController {

    private final GetInvestorPortfolioUseCase getInvestorPortfolioUseCase;
    private final GetFamilyMfPortfolioUseCase getFamilyMfPortfolioUseCase;
    private final GetFolioMasterSummaryUseCase getFolioMasterSummaryUseCase;
    private final ConvertToMobilePortfolioUseCase convertToMobilePortfolioUseCase;
    private final GetInvestorTaxReportUseCase getInvestorTaxReportUseCase;
    private final ConvertToCapitalGainReportUseCase convertToCapitalGainReportUseCase;

    @GetMapping("/getInvestorPortfolioNew")
    public ResponseEntity<?> getInvestorPortfolioNew(
            HttpServletRequest request,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String financial_year,
            @RequestParam(required = false) String folio_type,
            @RequestParam(required = false) String selected_date,
            @RequestParam(required = false) String summary_type,
            @RequestParam(required = false) String report_type,
            @RequestParam(required = false) String source) throws Exception
    {
        try
        {
            userId = MfboxUtils.checkParem(userId);
            clientName = MfboxUtils.checkParem(clientName);
            financial_year = MfboxUtils.checkParem(financial_year);
            folio_type = MfboxUtils.checkParem(folio_type);
            selected_date = MfboxUtils.checkParem(selected_date);
            summary_type = MfboxUtils.checkParem(summary_type);
            source = MfboxUtils.checkParem(source);
            report_type = MfboxUtils.checkParem(report_type);

            if (report_type.equalsIgnoreCase("family")) {
                String fy = financial_year.isEmpty() ? "All" : financial_year;
                String ft = folio_type.isEmpty() ? "All" : folio_type;
                String pg = summary_type.isEmpty() ? "All" : summary_type;
                FamilyMfPortfolioResponse familyResponse = getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                        Integer.parseInt(userId), fy, ft, selected_date, clientName, pg);
                return ResponseEntity.ok(familyResponse);
            }

            if (report_type.equalsIgnoreCase("folio")) {
                InvestorPortfolioResponse portfolio = getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                        Integer.parseInt(userId), financial_year, folio_type, selected_date, clientName, summary_type);
                FolioMasterSummaryResponse folioResponse = getFolioMasterSummaryUseCase.buildFolioMasterSummary(
                        portfolio, Integer.parseInt(userId), clientName);
                return ResponseEntity.ok(folioResponse);
            }

            InvestorPortfolioResponse investorPortfolioResponse = getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    Integer.parseInt(userId), financial_year, folio_type, selected_date, clientName, summary_type);

            if (source.equalsIgnoreCase("mobile"))
            {
                System.out.println("mobile");
                InvestorPortfolioNewMobileResponse mobileResponse =
                        convertToMobilePortfolioUseCase.convert(investorPortfolioResponse);
                return ResponseEntity.ok(mobileResponse);
            }

            return ResponseEntity.ok(investorPortfolioResponse);
        } catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/getTaxReportsNew")
    public ResponseEntity<?> getTaxReportsNew(
            @RequestParam("userId") Integer userId,
            @RequestParam("clientName") String clientName,
            @RequestParam("option") String option,
            @RequestParam(value = "financialYear", defaultValue = "All") String financialYear,
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate,
            @RequestParam(value = "source") String source) {

        List<InvestorSchemeWiseTransactionTaxReport> result =
                getInvestorTaxReportUseCase.getInvestorTaxReportNew(
                        userId, clientName, option, financialYear, startDate, endDate);

        if (source != null && source.equalsIgnoreCase("mobile")) {
            CapitalGainReportApiResponse newResponse =
                    convertToCapitalGainReportUseCase.convert(result);
            return ResponseEntity.ok(newResponse);
        }

        return ResponseEntity.ok(result);
    }
}
