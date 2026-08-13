package com.hexagonal.portfolio.application.port.out;

import java.util.List;

/** Loads the latest published NAV for a scheme. */
public interface LoadLatestNavPort {

    List<Object[]> findLatestNavNews(String schemeCode);
}
