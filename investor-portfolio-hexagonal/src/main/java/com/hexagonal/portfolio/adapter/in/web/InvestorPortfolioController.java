package com.hexagonal.portfolio.adapter.in.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hexagonal.portfolio.application.port.in.ConvertToCapitalGainReportUseCase;
import com.hexagonal.portfolio.application.port.in.ConvertToMobilePortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetFamilyMfPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetFolioMasterSummaryUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorTaxReportUseCase;
import com.hexagonal.portfolio.domain.exception.InvalidRequestException;
import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse;
import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse;
import com.hexagonal.portfolio.domain.model.FolioMasterSummaryResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;
import com.hexagonal.portfolio.domain.service.MfboxUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Driving adapter for the investor-portfolio read model.
 *
 * <p>Routing is unchanged from the legacy {@code /investor/getInvestorPortfolioNew}: same path, same
 * optional request parameters, same {@code checkParem} normalisation, and the same
 * {@code report_type} / {@code source} branching, so success payloads stay equivalent.
 *
 * <p>Failure handling is not unchanged, and deliberately so. The blanket
 * {@code catch (Exception e) { printStackTrace(); return badRequest(); }} that used to wrap the first
 * endpoint is gone: it reported server bugs as client errors and left the second endpoint, which
 * caught nothing, behaving differently from the first. Both now delegate to
 * {@link GlobalExceptionHandler}. Parameters carry declared constraints, so a bad request is
 * rejected before any use case runs rather than by an exception thrown mid-valuation.
 *
 * <p>{@code userId} stays a {@code String} rather than becoming an {@code Integer}: the legacy
 * contract accepts the literals {@code "null"} and {@code "undefined"} and normalises them through
 * {@code checkParem}, which binding to {@code Integer} would turn into a type-mismatch before
 * {@code checkParem} ever ran.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/investor")
@RequiredArgsConstructor
public class InvestorPortfolioController {

    /** Digits, optionally padded — {@code checkParem} trims before the value is parsed. */
    private static final String NUMERIC_ID = "\\s*\\d+\\s*";

    private final GetInvestorPortfolioUseCase getInvestorPortfolioUseCase;
    private final GetFamilyMfPortfolioUseCase getFamilyMfPortfolioUseCase;
    private final GetFolioMasterSummaryUseCase getFolioMasterSummaryUseCase;
    private final ConvertToMobilePortfolioUseCase convertToMobilePortfolioUseCase;
    private final GetInvestorTaxReportUseCase getInvestorTaxReportUseCase;
    private final ConvertToCapitalGainReportUseCase convertToCapitalGainReportUseCase;

    @GetMapping("/getInvestorPortfolioNew")
    public ResponseEntity<?> getInvestorPortfolioNew(
            @RequestParam(required = false)
            @NotBlank(message = "is required")
            @Pattern(regexp = NUMERIC_ID, message = "must be numeric") String userId,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String financial_year,
            @RequestParam(required = false) String folio_type,
            @RequestParam(required = false) String selected_date,
            @RequestParam(required = false) String summary_type,
            @RequestParam(required = false) String report_type,
            @RequestParam(required = false) String source) {

        // Normalised copies rather than reassigned parameters: the originals stay available for
        // logging, and the request values are visibly immutable from here down.
        String normalisedUserId = MfboxUtils.checkParem(userId);
        String client = MfboxUtils.checkParem(clientName);
        String financialYear = MfboxUtils.checkParem(financial_year);
        String folioType = MfboxUtils.checkParem(folio_type);
        String selectedDate = MfboxUtils.checkParem(selected_date);
        String summaryType = MfboxUtils.checkParem(summary_type);
        String normalisedSource = MfboxUtils.checkParem(source);
        String reportType = MfboxUtils.checkParem(report_type);

        int investorId = parseInvestorId(normalisedUserId);

        if (reportType.equalsIgnoreCase("family")) {
            String fy = financialYear.isEmpty() ? "All" : financialYear;
            String ft = folioType.isEmpty() ? "All" : folioType;
            String pg = summaryType.isEmpty() ? "All" : summaryType;
            FamilyMfPortfolioResponse familyResponse = getFamilyMfPortfolioUseCase.getFamilyPortfolio(
                    investorId, fy, ft, selectedDate, client, pg);
            return ResponseEntity.ok(familyResponse);
        }

        if (reportType.equalsIgnoreCase("folio")) {
            InvestorPortfolioResponse portfolio = getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                    investorId, financialYear, folioType, selectedDate, client, summaryType);
            FolioMasterSummaryResponse folioResponse = getFolioMasterSummaryUseCase.buildFolioMasterSummary(
                    portfolio, investorId, client);
            return ResponseEntity.ok(folioResponse);
        }

        InvestorPortfolioResponse investorPortfolioResponse = getInvestorPortfolioUseCase.getInvestorPortfolioNew(
                investorId, financialYear, folioType, selectedDate, client, summaryType);

        if (normalisedSource.equalsIgnoreCase("mobile")) {
            log.debug("Converting portfolio for userId={} into the mobile payload", investorId);
            InvestorPortfolioNewMobileResponse mobileResponse =
                    convertToMobilePortfolioUseCase.convert(investorPortfolioResponse);
            return ResponseEntity.ok(mobileResponse);
        }

        return ResponseEntity.ok(investorPortfolioResponse);
    }

    @GetMapping("/getTaxReportsNew")
    public ResponseEntity<?> getTaxReportsNew(
            @RequestParam("userId") @NotNull(message = "is required") Integer userId,
            @RequestParam("clientName") @NotBlank(message = "is required") String clientName,
            @RequestParam("option") @NotBlank(message = "is required") String option,
            @RequestParam(value = "financialYear", defaultValue = "All") String financialYear,
            @RequestParam(value = "startDate", defaultValue = "") String startDate,
            @RequestParam(value = "endDate", defaultValue = "") String endDate,
            @RequestParam("source") @NotBlank(message = "is required") String source) {

        List<InvestorSchemeWiseTransactionTaxReport> result =
                getInvestorTaxReportUseCase.getInvestorTaxReportNew(
                        userId, clientName, option, financialYear, startDate, endDate);

        if (source.equalsIgnoreCase("mobile")) {
            CapitalGainReportApiResponse newResponse =
                    convertToCapitalGainReportUseCase.convert(result);
            return ResponseEntity.ok(newResponse);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Parses the normalised investor id.
     *
     * <p>The {@code @Pattern} constraint above should already have rejected anything non-numeric.
     * This is the backstop for the paths where method validation is not in play — a standalone
     * {@code MockMvc} setup, or a direct call — so that a bad id is a 400 by construction rather
     * than by configuration. The raw value is not echoed back.
     */
    private int parseInvestorId(String normalisedUserId) {
        try {
            return Integer.parseInt(normalisedUserId.trim());
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("userId must be a numeric investor id", ex);
        }
    }
}
