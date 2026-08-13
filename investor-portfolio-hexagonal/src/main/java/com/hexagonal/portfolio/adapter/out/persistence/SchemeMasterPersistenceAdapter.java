package com.hexagonal.portfolio.adapter.out.persistence;

import com.hexagonal.portfolio.application.port.out.LoadFundScorePort;
import com.hexagonal.portfolio.application.port.out.LoadHealthCheckupPort;
import com.hexagonal.portfolio.application.port.out.LoadLatestNavPort;
import com.hexagonal.portfolio.application.port.out.LoadNavHistoryPort;
import com.hexagonal.portfolio.application.port.out.LoadSchemeMasterPort;
import com.hexagonal.portfolio.domain.model.FundScoreInfo;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.AmfiLatestNavRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.AmfiMfNavRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.AmfiSchemeMasterRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.InvestorPortfolioHealthCheckupRepository;
import com.hexagonal.portfolio.adapter.out.persistence.repository.amfi.SchemeMonthlyFundScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads the AMFI reference schema. These queries already project scalar rows, so there
 * is no entity to translate — the raw projections are handed straight to the use case.
 */
@Component
@RequiredArgsConstructor
class SchemeMasterPersistenceAdapter
        implements LoadSchemeMasterPort, LoadLatestNavPort, LoadNavHistoryPort,
                   LoadFundScorePort, LoadHealthCheckupPort {

    private final AmfiSchemeMasterRepository amfiSchemeMasterRepository;
    private final AmfiLatestNavRepository amfiLatestNavRepository;
    private final AmfiMfNavRepository amfiMfNavRepository;
    private final SchemeMonthlyFundScoreRepository schemeMonthlyFundScoreRepository;
    private final InvestorPortfolioHealthCheckupRepository investorPortfolioHealthCheckupRepository;

    @Override
    public List<Object[]> findSchemeMappingNew1(String src, String productCode) {
        return amfiSchemeMasterRepository.findSchemeMappingNew1(src, productCode);
    }

    @Override
    public List<Object[]> findByAmfiCodeNew1(String amfiCode) {
        return amfiSchemeMasterRepository.findByAmfiCodeNew1(amfiCode);
    }

    @Override
    public List<Object[]> findLatestNavNews(String schemeCode) {
        return amfiLatestNavRepository.findLatestNavNews(schemeCode);
    }

    @Override
    public List<Object[]> findLastTwoNavsOnOrBeforeNew(String schemeCode, String endDate) {
        return amfiMfNavRepository.findLastTwoNavsOnOrBeforeNew(schemeCode, endDate);
    }

    @Override
    public FundScoreInfo findLatestScoreNew(String schemeAmfiCode) {
        return schemeMonthlyFundScoreRepository.findLatestScoreNew(schemeAmfiCode);
    }

    @Override
    public List<Integer> findRatingNew(String schemeAmfiCode) {
        return investorPortfolioHealthCheckupRepository.findRatingNew(schemeAmfiCode);
    }

    @Override
    public List<Object[]> findSchemeMasterByProductCodeNew(String src, String productCode) {
        return amfiSchemeMasterRepository.findSchemeMasterByProductCodeNew(src, productCode);
    }

    @Override
    public List<Double> findLatestNav(String schemeCode, String navDate) {
        return amfiMfNavRepository.findLatestNav(schemeCode, navDate);
    }
}
