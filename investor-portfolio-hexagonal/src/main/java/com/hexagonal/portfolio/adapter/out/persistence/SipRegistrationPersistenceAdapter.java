package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadSipRegistrationPort;
import com.hexagonal.portfolio.domain.model.InvestorSipCams49;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorSipCamsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class SipRegistrationPersistenceAdapter implements LoadSipRegistrationPort {

    private final InvestorSipCamsRepository investorSipCamsRepository;

    @Override
    public List<InvestorSipCams49> findActiveStpSwpNew(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorSipCamsRepository.findActiveStpSwpNew(userId, clientName), InvestorSipCams49.class);
    }

    @Override
    public List<InvestorSipCams49> findActiveSipNew(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorSipCamsRepository.findActiveSipNew(userId, clientName), InvestorSipCams49.class);
    }

    @Override
    public List<InvestorSipCams49> findClosedSipStpSwpNew(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorSipCamsRepository.findClosedSipStpSwpNew(userId, clientName), InvestorSipCams49.class);
    }
}
