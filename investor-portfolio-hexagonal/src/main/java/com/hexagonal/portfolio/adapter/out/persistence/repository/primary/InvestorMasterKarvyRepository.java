package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorMasterKarvy;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorMasterKarvyRepository extends JpaRepository<InvestorMasterKarvy, Integer> {

    @Query("FROM InvestorMasterKarvy m WHERE m.user_id = :userId AND m.client_name = :clientName")
    List<InvestorMasterKarvy> findByUser_idAndClient_nameNew(@Param("userId") Integer userId, @Param("clientName") String clientName);

    @Query("""
    SELECT i
    FROM InvestorMasterKarvy i
    WHERE i.user_id = :userId
      AND i.client_name = :clientName
    ORDER BY i.id DESC
""")
    List<InvestorMasterKarvy> findByUserIdAndClientNameOrderByIdDesc(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );
}
