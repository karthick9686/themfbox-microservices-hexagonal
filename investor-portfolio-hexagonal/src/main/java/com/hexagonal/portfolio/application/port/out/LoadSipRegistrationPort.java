package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorSipCams49;

import java.util.List;

/** Loads SIP/STP/SWP registrations used to flag systematic schemes. */
public interface LoadSipRegistrationPort {

    List<InvestorSipCams49> findActiveStpSwpNew(Integer userId, String clientName);

    List<InvestorSipCams49> findActiveSipNew(Integer userId, String clientName);

    List<InvestorSipCams49> findClosedSipStpSwpNew(Integer userId, String clientName);
}
