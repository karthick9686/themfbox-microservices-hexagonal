package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorMasterCams;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorMasterCamsRepository extends JpaRepository<InvestorMasterCams, Integer> {

    @Query("FROM InvestorMasterCams m WHERE m.user_id = :userId AND m.client_name = :clientName")
    List<InvestorMasterCams> findByUser_idAndClient_nameNew(@Param("userId") Integer userId, @Param("clientName") String clientName);

    @Query("""
    SELECT i
    FROM InvestorMasterCams i
    WHERE i.user_id = :userId
      AND i.client_name = :clientName
    ORDER BY i.id DESC
""")
    List<InvestorMasterCams> findData(
            @Param("userId") Integer userId,
            @Param("clientName") String clientName
    );
}
