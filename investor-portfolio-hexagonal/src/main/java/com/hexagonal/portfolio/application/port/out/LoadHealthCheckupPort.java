package com.hexagonal.portfolio.application.port.out;

import java.util.List;

/** Loads portfolio health-checkup star ratings for a scheme. */
public interface LoadHealthCheckupPort {

    List<Integer> findRatingNew(String schemeAmfiCode);
}
