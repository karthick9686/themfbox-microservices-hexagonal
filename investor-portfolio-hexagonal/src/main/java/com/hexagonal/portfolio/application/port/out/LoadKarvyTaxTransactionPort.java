package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;

import java.util.Date;
import java.util.List;

/** Karvy reads specific to capital-gain reporting, including segregated-scheme companions. */
public interface LoadKarvyTaxTransactionPort {

    List<Object[]> findDistinctSchemes(Integer userId, List<String> trxnTypes, Date endDate, String clientName);

    List<InvestorTransactionKarvy> findSchemeTransactions(Integer userId, String folioNumber, String productCode,
                                                          Date endDate, String clientName);

    List<InvestorTransactionKarvy> findSegregatedTransactionsNew(Integer user_id, String folio_number,
                                                                 String product_code, Date financial_year_end_date,
                                                                 String client_name);
}
