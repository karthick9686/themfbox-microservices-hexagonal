package com.hexagonal.portfolio.application.port.out;

import java.util.List;

/**
 * Loads the cost-inflation index applicable on or before a date, most recent first.
 *
 * <p>The legacy repository took a {@code Pageable} to cap the result at one row; that
 * paging concern now lives in the adapter so the application layer stays free of
 * Spring Data types. The returned list is capped identically.
 */
public interface LoadInflationIndexPort {

    List<Double> findInflationIndex(String date);
}
