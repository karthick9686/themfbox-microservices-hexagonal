package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;

import java.util.Date;
import java.util.List;

/** Loads CAMS-registrar transactions up to the valuation cut-off date. */
public interface LoadCamsTransactionPort {

    List<InvestorTransactionCams> findAllByUserAndDate(Integer userId, String clientName, Date cutoffDate);
}
