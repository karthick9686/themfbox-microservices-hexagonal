package com.hexagonal.portfolio.adapter.out.persistence.repository.amfi;

import com.hexagonal.portfolio.adapter.out.persistence.entity.amfi.InvestorPortfolioHealthCheckup;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorPortfolioHealthCheckupRepository extends JpaRepository<InvestorPortfolioHealthCheckup, Long> {

    @Query("select h.rating_value from InvestorPortfolioHealthCheckup h where h.scheme_amfi_code = :schemeAmfiCode")
    List<Integer> findRatingNew(@Param("schemeAmfiCode") String schemeAmfiCode);
}
