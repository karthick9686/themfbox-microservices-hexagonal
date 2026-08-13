package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadCamsTaxTransactionPort;
import com.hexagonal.portfolio.domain.model.InvestorTransactionCams;
import com.hexagonal.portfolio.adapter.out.persistence.repository.primary.InvestorTransactionCamsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
class CamsTaxTransactionPersistenceAdapter implements LoadCamsTaxTransactionPort {

    private final InvestorTransactionCamsRepository investorTransactionCamsRepository;

    @Override
    public List<Object[]> findDistinctSchemes(Integer userId, List<String> trxnTypes, Date endDate, String clientName) {
        return investorTransactionCamsRepository.findDistinctSchemes(userId, trxnTypes, endDate, clientName);
    }

    @Override
    public List<InvestorTransactionCams> findSchemeTransactionsPriceAsc(Integer userId, String folioNo, String prodcode,
                                                                       Date endDate, String clientName) {
        return PersistenceMapper.mapList(
                investorTransactionCamsRepository.findSchemeTransactionsPriceAsc(
                        userId, folioNo, prodcode, endDate, clientName),
                InvestorTransactionCams.class);
    }

    @Override
    public List<InvestorTransactionCams> findSchemeTransactionsPriceDesc(Integer userId, String folioNo, String prodcode,
                                                                        Date endDate, String clientName) {
        return PersistenceMapper.mapList(
                investorTransactionCamsRepository.findSchemeTransactionsPriceDesc(
                        userId, folioNo, prodcode, endDate, clientName),
                InvestorTransactionCams.class);
    }
}
