package com.hexagonal.portfolio.application.port.out;

import java.util.List;

/**
 * Resolves an RTA product code to the AMFI scheme master.
 * Rows are returned in the column order the legacy projection used.
 */
public interface LoadSchemeMasterPort {

    List<Object[]> findSchemeMappingNew1(String src, String productCode);

    List<Object[]> findByAmfiCodeNew1(String amfiCode);

    List<Object[]> findSchemeMasterByProductCodeNew(String src, String productCode);
}
