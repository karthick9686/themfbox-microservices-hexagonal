package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadKarvyFolioMasterPort;
import com.hexagonal.portfolio.domain.model.InvestorMasterKarvy;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorMasterKarvyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class KarvyFolioMasterPersistenceAdapter implements LoadKarvyFolioMasterPort {

    private final InvestorMasterKarvyRepository investorMasterKarvyRepository;

    @Override
    public List<InvestorMasterKarvy> findByUser_idAndClient_nameNew(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorMasterKarvyRepository.findByUser_idAndClient_nameNew(userId, clientName),
                InvestorMasterKarvy.class);
    }

    @Override
    public List<InvestorMasterKarvy> findByUserIdAndClientNameOrderByIdDesc(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorMasterKarvyRepository.findByUserIdAndClientNameOrderByIdDesc(userId, clientName),
                InvestorMasterKarvy.class);
    }
}
