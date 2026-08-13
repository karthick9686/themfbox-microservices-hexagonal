package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadManualTransactionPort;
import com.hexagonal.portfolio.domain.model.PortfolioTransactions;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.PortfolioTransactionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
class ManualTransactionPersistenceAdapter implements LoadManualTransactionPort {

    private final PortfolioTransactionsRepository portfolioTransactionsRepository;

    @Override
    public List<PortfolioTransactions> findAllByUserAndDateNew(Integer userId, String clientName, Date endDate) {
        return PersistenceMapper.mapList(
                portfolioTransactionsRepository.findAllByUserAndDateNew(userId, clientName, endDate),
                PortfolioTransactions.class);
    }

    @Override
    public List<PortfolioTransactions> findByFolioAndSchemeAndUserAndDateNew1(
            String folioNo, String schemeCode, Integer userId, String clientName, Date endDate) {
        return PersistenceMapper.mapList(
                portfolioTransactionsRepository.findByFolioAndSchemeAndUserAndDateNew1(
                        folioNo, schemeCode, userId, clientName, endDate),
                PortfolioTransactions.class);
    }
}
