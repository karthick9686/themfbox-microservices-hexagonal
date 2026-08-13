package com.hexagonal.portfolio.application.port.in;

import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;

/**
 * Primary port: values an investor's mutual-fund portfolio as at a cut-off date.
 *
 * <p>The parameter list is kept identical to the legacy service so the valuation
 * semantics (financial year, folio type, selected date, page) carry over unchanged.
 */
public interface GetInvestorPortfolioUseCase {

    InvestorPortfolioResponse getInvestorPortfolioNew(
            Integer user_id, String financial_year, String folio_type,
            String selected_date, String client_name, String page);
}
