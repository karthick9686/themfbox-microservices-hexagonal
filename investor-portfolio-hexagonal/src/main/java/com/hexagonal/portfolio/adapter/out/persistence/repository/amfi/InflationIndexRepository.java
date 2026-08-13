package com.hexagonal.portfolio.adapter.out.persistence.repository.amfi;

import com.hexagonal.portfolio.adapter.out.persistence.entity.amfi.InflationIndex;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InflationIndexRepository extends JpaRepository<InflationIndex, Long>
{

    @Query("select i.cost_inflation_index from InflationIndex i where i.start_year <= :date order by i.start_year desc")
    List<Double> findInflationIndex(@Param("date") String date, Pageable pageable);
}
