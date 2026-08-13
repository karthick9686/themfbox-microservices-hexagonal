package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadKarvyTransactionPort;
import com.hexagonal.portfolio.domain.model.InvestorTransactionKarvy;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorTransactionKarvyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
class KarvyTransactionPersistenceAdapter implements LoadKarvyTransactionPort {

    private final InvestorTransactionKarvyRepository investorTransactionKarvyRepository;

    @Override
    public List<InvestorTransactionKarvy> findAllByUserAndDateNew(Integer userId, String clientName, Date endDate) {
        return PersistenceMapper.mapList(
                investorTransactionKarvyRepository.findAllByUserAndDateNew(userId, clientName, endDate),
                InvestorTransactionKarvy.class);
    }
}
