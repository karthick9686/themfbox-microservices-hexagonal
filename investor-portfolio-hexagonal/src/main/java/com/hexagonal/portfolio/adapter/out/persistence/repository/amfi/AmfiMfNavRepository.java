package com.hexagonal.portfolio.adapter.out.persistence.repository.amfi;

import com.hexagonal.portfolio.adapter.out.persistence.entity.amfi.AmfiMfNav;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AmfiMfNavRepository extends JpaRepository<AmfiMfNav, Integer> {

    @Query(value = "select nav_date, net_asset_value from amfi_mf_nav " +
            "where scheme_code = :schemeCode and nav_date <= :endDate " +
            "order by nav_date desc limit 2", nativeQuery = true)
    List<Object[]> findLastTwoNavsOnOrBeforeNew(@Param("schemeCode") String schemeCode,
                                             @Param("endDate") String endDate);

    @Query(value = "select net_asset_value from amfi_mf_nav " +
            "where scheme_code = :schemeCode and nav_date <= :navDate " +
            "order by nav_date desc limit 1", nativeQuery = true)
    List<Double> findLatestNav(@Param("schemeCode") String schemeCode,
                               @Param("navDate") String navDate);
}
