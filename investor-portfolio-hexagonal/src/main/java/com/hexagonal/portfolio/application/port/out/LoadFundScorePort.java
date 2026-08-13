package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.FundScoreInfo;

/** Loads the latest monthly fund score for a scheme. */
public interface LoadFundScorePort {

    FundScoreInfo findLatestScoreNew(String schemeAmfiCode);
}
