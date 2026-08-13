package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadCamsTransactionPort;
import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorTransactionCamsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
class CamsTransactionPersistenceAdapter implements LoadCamsTransactionPort {

    private final InvestorTransactionCamsRepository investorTransactionCamsRepository;

    @Override
    public List<InvestorTransactionCams> findAllByUserAndDate(Integer userId, String clientName, Date cutoffDate) {
        return PersistenceMapper.mapList(
                investorTransactionCamsRepository.findAllByUserAndDate(userId, clientName, cutoffDate),
                InvestorTransactionCams.class);
    }
}
