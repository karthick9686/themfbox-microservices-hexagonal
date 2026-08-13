package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.PortfolioTransactions;

import java.util.Date;
import java.util.List;

/** Loads manually uploaded / CAS-imported transactions. */
public interface LoadManualTransactionPort {

    List<PortfolioTransactions> findAllByUserAndDateNew(Integer userId, String clientName, Date endDate);

    List<PortfolioTransactions> findByFolioAndSchemeAndUserAndDateNew1(String folioNo, String schemeCode,
                                                                      Integer userId, String clientName, Date endDate);
}
