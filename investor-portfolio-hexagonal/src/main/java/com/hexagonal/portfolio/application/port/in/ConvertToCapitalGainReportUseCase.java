package com.hexagonal.portfolio.application.port.in;

import com.hexagonal.portfolio.domain.model.CapitalGainReportApiResponse;
import com.hexagonal.portfolio.domain.model.InvestorSchemeWiseTransactionTaxReport;

import java.util.List;

/** Primary port: condenses raw tax-report rows into the mobile capital-gain payload. */
public interface ConvertToCapitalGainReportUseCase {

    CapitalGainReportApiResponse convert(List<InvestorSchemeWiseTransactionTaxReport> rawList);
}
