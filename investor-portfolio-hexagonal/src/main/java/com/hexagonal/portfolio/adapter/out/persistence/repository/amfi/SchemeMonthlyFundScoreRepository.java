package com.hexagonal.portfolio.adapter.out.persistence.repository.amfi;

import com.hexagonal.portfolio.adapter.out.persistence.entity.amfi.SchemeMonthlyFundScore;
import com.hexagonal.portfolio.domain.model.FundScoreInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeMonthlyFundScoreRepository extends JpaRepository<SchemeMonthlyFundScore, Integer> {

    @Query(value = "SELECT * FROM scheme_monthly_fund_score " +
            "WHERE scheme_amfi_code = :schemeAmfiCode " +
            "ORDER BY consistency_score_year DESC, consistency_score_month DESC, id DESC " +
            "LIMIT 1", nativeQuery = true)
    FundScoreInfo findLatestScoreNew(@Param("schemeAmfiCode") String schemeAmfiCode);
}
