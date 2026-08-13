package com.hexagonal.portfolio.application.port.in;

import com.hexagonal.portfolio.domain.model.FamilyMfPortfolioResponse;

/** Primary port: family-grouped roll-up across every investor mapped to a family head. */
public interface GetFamilyMfPortfolioUseCase {

    FamilyMfPortfolioResponse getFamilyPortfolio(
            Integer familyHeadId, String financial_year, String folio_type,
            String selected_date, String clientName, String page);
}
