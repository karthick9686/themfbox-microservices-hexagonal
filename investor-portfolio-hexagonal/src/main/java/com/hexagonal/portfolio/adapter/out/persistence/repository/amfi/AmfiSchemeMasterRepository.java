package com.hexagonal.portfolio.adapter.out.persistence.repository.amfi;

import com.hexagonal.portfolio.adapter.out.persistence.entity.amfi.AmfiSchemeMaster;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AmfiSchemeMasterRepository extends JpaRepository<AmfiSchemeMaster, Integer> {

    @Query(value = "select sm.scheme_company, sm.scheme_advisorkhoj_category, sm.scheme_broad_category, " +
            "sm.scheme_amfi_code, sm.scheme_amfi, sm.isin_no, sm.scheme_amfi_common, " +
            "sm.scheme_amfi_short_name, sm.riskometer " +
            "from amfi_scheme_master sm join amfi_scheme_master_productcode_map m " +
            "on m.scheme_amfi_code = sm.scheme_amfi_code " +
            "where m.src = :src and m.product_code = :productCode limit 1", nativeQuery = true)
    List<Object[]> findSchemeMappingNew1(@Param("src") String src, @Param("productCode") String productCode);

    @Query(value = "select sm.scheme_company, sm.scheme_advisorkhoj_category, sm.scheme_broad_category, " +
            "sm.scheme_amfi_code, sm.scheme_amfi, sm.isin_no, sm.scheme_amfi_common, " +
            "sm.scheme_amfi_short_name, sm.riskometer " +
            "from amfi_scheme_master sm where sm.scheme_amfi_code = :amfiCode limit 1", nativeQuery = true)
    List<Object[]> findByAmfiCodeNew1(@Param("amfiCode") String amfiCode);

    @Query(value = "select sm.tax_category, sm.scheme_amfi, sm.scheme_amfi_code, sm.isin_no, " +
            "sm.isin_divreinvst_no, sm.scheme_amfi_short_name, sm.scheme_advisorkhoj_category " +
            "from amfi_scheme_master sm " +
            "join amfi_scheme_master_productcode_map m on m.scheme_amfi_code = sm.scheme_amfi_code " +
            "where m.src = :src and m.product_code = :productCode limit 1",
            nativeQuery = true)
    List<Object[]> findSchemeMasterByProductCodeNew(@Param("src") String src,
                                                 @Param("productCode") String productCode);
}
