package com.hexagonal.portfolio.application.port.in;

import com.hexagonal.portfolio.domain.model.FolioMasterSummaryResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;

/** Primary port: scheme-wise folio master summary for a computed portfolio. */
public interface GetFolioMasterSummaryUseCase {

    FolioMasterSummaryResponse buildFolioMasterSummary(
            InvestorPortfolioResponse portfolio, Integer userId, String clientName);
}
