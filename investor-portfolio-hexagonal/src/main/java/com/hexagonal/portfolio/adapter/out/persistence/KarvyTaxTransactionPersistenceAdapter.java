package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadKarvyTaxTransactionPort;
import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorTransactionKarvyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
class KarvyTaxTransactionPersistenceAdapter implements LoadKarvyTaxTransactionPort {

    private final InvestorTransactionKarvyRepository investorTransactionKarvyRepository;

    @Override
    public List<Object[]> findDistinctSchemes(Integer userId, List<String> trxnTypes, Date endDate, String clientName) {
        return investorTransactionKarvyRepository.findDistinctSchemes(userId, trxnTypes, endDate, clientName);
    }

    @Override
    public List<InvestorTransactionKarvy> findSchemeTransactions(Integer userId, String folioNumber, String productCode,
                                                                 Date endDate, String clientName) {
        return PersistenceMapper.mapList(
                investorTransactionKarvyRepository.findSchemeTransactions(
                        userId, folioNumber, productCode, endDate, clientName),
                InvestorTransactionKarvy.class);
    }

    @Override
    public List<InvestorTransactionKarvy> findSegregatedTransactionsNew(Integer user_id, String folio_number,
                                                                        String product_code, Date financial_year_end_date,
                                                                        String client_name) {
        return PersistenceMapper.mapList(
                investorTransactionKarvyRepository.findSegregatedTransactionsNew(
                        user_id, folio_number, product_code, financial_year_end_date, client_name),
                InvestorTransactionKarvy.class);
    }
}
