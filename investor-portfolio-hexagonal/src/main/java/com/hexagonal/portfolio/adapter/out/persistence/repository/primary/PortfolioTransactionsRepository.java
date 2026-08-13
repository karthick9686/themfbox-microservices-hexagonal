package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.PortfolioTransactions;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioTransactionsRepository extends JpaRepository<PortfolioTransactions, Long> {

    @Query("FROM PortfolioTransactions t WHERE t.user_id = :userId AND t.client_name = :clientName " +
            "AND t.traddate <= :endDate")
    List<PortfolioTransactions> findAllByUserAndDateNew(@Param("userId") Integer userId,
                                                        @Param("clientName") String clientName,
                                                        @Param("endDate") Date endDate);

    @Query("FROM PortfolioTransactions t WHERE t.folio_no = :folioNo AND t.scheme_code = :schemeCode " +
            "AND t.user_id = :userId AND t.client_name = :clientName AND t.traddate <= :endDate " +
            "ORDER BY t.traddate ASC, t.units DESC")
    List<PortfolioTransactions> findByFolioAndSchemeAndUserAndDateNew1(@Param("folioNo") String folioNo,
                                                                   @Param("schemeCode") String schemeCode,
                                                                   @Param("userId") Integer userId,
                                                                   @Param("clientName") String clientName,
                                                                   @Param("endDate") Date endDate);
}
