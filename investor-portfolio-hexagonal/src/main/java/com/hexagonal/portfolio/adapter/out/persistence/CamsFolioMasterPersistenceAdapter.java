package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadCamsFolioMasterPort;
import com.hexagonal.portfolio.domain.model.InvestorMasterCams;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorMasterCamsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class CamsFolioMasterPersistenceAdapter implements LoadCamsFolioMasterPort {

    private final InvestorMasterCamsRepository investorMasterCamsRepository;

    @Override
    public List<InvestorMasterCams> findByUser_idAndClient_nameNew(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorMasterCamsRepository.findByUser_idAndClient_nameNew(userId, clientName),
                InvestorMasterCams.class);
    }

    @Override
    public List<InvestorMasterCams> findData(Integer userId, String clientName) {
        return PersistenceMapper.mapList(
                investorMasterCamsRepository.findData(userId, clientName), InvestorMasterCams.class);
    }
}
