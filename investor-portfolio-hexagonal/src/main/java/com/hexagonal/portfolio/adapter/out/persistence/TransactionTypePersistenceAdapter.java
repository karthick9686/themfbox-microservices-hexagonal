package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadTransactionTypePort;
import com.hexagonal.portfolio.domain.model.TransactionType;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class TransactionTypePersistenceAdapter implements LoadTransactionTypePort {

    private final TransactionTypeRepository transactionTypeRepository;

    @Override
    public List<TransactionType> findAll() {
        return PersistenceMapper.mapList(transactionTypeRepository.findAll(), TransactionType.class);
    }
}
