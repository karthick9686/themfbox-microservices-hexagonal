package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.User;

import java.util.Optional;

/** Loads the investor whose portfolio is being valued. */
public interface LoadInvestorPort {

    Optional<User> findByIdAndClientName(Integer id, String clientName);
}
