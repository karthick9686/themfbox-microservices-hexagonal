package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;

import java.util.Date;
import java.util.List;

/** Loads Karvy/KFin-registrar transactions up to the valuation cut-off date. */
public interface LoadKarvyTransactionPort {

    List<InvestorTransactionKarvy> findAllByUserAndDateNew(Integer userId, String clientName, Date endDate);
}
