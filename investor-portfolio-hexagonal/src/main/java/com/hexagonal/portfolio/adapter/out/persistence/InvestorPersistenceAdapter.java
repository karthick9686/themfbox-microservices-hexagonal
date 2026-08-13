package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadInvestorPort;
import com.hexagonal.portfolio.domain.model.User;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class InvestorPersistenceAdapter implements LoadInvestorPort {

    private final UsersRepository usersRepository;

    @Override
    public Optional<User> findByIdAndClientName(Integer id, String clientName) {
        return usersRepository.findByIdAndClientName(id, clientName)
                .map(entity -> PersistenceMapper.map(entity, User.class));
    }
}
