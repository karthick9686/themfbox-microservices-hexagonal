package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadFamilyMappingPort;
import com.hexagonal.portfolio.domain.model.UsersMapping;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.UsersMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class FamilyMappingPersistenceAdapter implements LoadFamilyMappingPort {

    private final UsersMappingRepository usersMappingRepository;

    @Override
    public List<UsersMapping> getFamilyMappedInvestor(Integer familyHeadId, String clientName) {
        return PersistenceMapper.mapList(
                usersMappingRepository.getFamilyMappedInvestor(familyHeadId, clientName), UsersMapping.class);
    }
}
