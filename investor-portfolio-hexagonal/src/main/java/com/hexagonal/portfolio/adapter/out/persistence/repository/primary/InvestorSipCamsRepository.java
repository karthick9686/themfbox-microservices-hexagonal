package com.hexagonal.portfolio.adapter.out.persistence.repository.primary;

import com.hexagonal.portfolio.adapter.out.persistence.entity.primary.InvestorSipCams49;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorSipCamsRepository extends JpaRepository<InvestorSipCams49, Integer> {

    @Query("FROM InvestorSipCams49 s WHERE s.user_id = :userId AND s.client_name = :clientName " +
            "AND s.aut_trntyp IN ('STP','SWP') AND s.sip_active = 1 AND s.cease_date IS NULL " +
            "AND s.from_date < CURRENT_DATE AND s.to_date > CURRENT_DATE")
    List<InvestorSipCams49> findActiveStpSwpNew(@Param("userId") Integer userId, @Param("clientName") String clientName);

    @Query("FROM InvestorSipCams49 s WHERE s.user_id = :userId AND s.client_name = :clientName " +
            "AND s.aut_trntyp = 'SIP' AND s.reg_date < CURRENT_DATE " +
            "AND (s.top_up_frq = '' OR s.from_date < CURRENT_DATE) " +
            "AND s.sip_active = 1 AND s.to_date > CURRENT_DATE")
    List<InvestorSipCams49> findActiveSipNew(@Param("userId") Integer userId, @Param("clientName") String clientName);

    @Query("FROM InvestorSipCams49 s WHERE s.user_id = :userId AND s.client_name = :clientName " +
            "AND s.aut_trntyp IN ('SIP','STP','SWP') AND s.reg_date < CURRENT_DATE " +
            "AND (s.sip_active = 0 OR s.to_date < CURRENT_DATE)")
    List<InvestorSipCams49> findClosedSipStpSwpNew(@Param("userId") Integer userId, @Param("clientName") String clientName);
}
