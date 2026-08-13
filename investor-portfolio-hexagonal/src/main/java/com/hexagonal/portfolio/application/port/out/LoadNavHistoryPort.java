package com.hexagonal.portfolio.application.port.out;

import java.util.List;

/** Loads the last two NAVs on or before a date, for day-change computation. */
public interface LoadNavHistoryPort {

    List<Object[]> findLastTwoNavsOnOrBeforeNew(String schemeCode, String endDate);

    List<Double> findLatestNav(String schemeCode, String navDate);
}
