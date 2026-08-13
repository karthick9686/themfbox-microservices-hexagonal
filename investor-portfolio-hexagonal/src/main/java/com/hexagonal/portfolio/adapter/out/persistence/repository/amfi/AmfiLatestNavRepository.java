package com.hexagonal.portfolio.adapter.out.persistence.repository.amfi;

import com.hexagonal.portfolio.adapter.out.persistence.entity.amfi.AmfiLatestNav;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AmfiLatestNavRepository extends JpaRepository<AmfiLatestNav, Integer> {

    @Query("select n.net_asset_value, n.nav_date, n.prev_net_asset_value, n.change_percent " +
            "from AmfiLatestNav n where n.scheme_code = :schemeCode")
    List<Object[]> findLatestNavNews(@Param("schemeCode") String schemeCode);
}
