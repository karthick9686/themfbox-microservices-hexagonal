package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;

import java.util.Date;
import java.util.List;

/** CAMS reads specific to capital-gain reporting (FIFO ordering by purchase price). */
public interface LoadCamsTaxTransactionPort {

    List<Object[]> findDistinctSchemes(Integer userId, List<String> trxnTypes, Date endDate, String clientName);

    List<InvestorTransactionCams> findSchemeTransactionsPriceAsc(Integer userId, String folioNo, String prodcode,
                                                                Date endDate, String clientName);

    List<InvestorTransactionCams> findSchemeTransactionsPriceDesc(Integer userId, String folioNo, String prodcode,
                                                                 Date endDate, String clientName);
}
