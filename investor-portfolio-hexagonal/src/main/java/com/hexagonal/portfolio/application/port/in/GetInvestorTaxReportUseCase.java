package com.hexagonal.portfolio.application.port.in;

import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;

import java.util.List;

/**
 * Primary port: scheme-wise realised capital-gain (tax) report for an investor over a
 * financial year or explicit date range.
 */
public interface GetInvestorTaxReportUseCase {

    List<InvestorSchemeWiseTransactionTaxReport> getInvestorTaxReportNew(
            Integer user_id, String client_name, String option, String financial_year,
            String start_date, String end_date);
}
