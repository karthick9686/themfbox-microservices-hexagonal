package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorMasterCams;

import java.util.List;

/** Loads CAMS folio master records (demat flags, KYC and nominee details). */
public interface LoadCamsFolioMasterPort {

    List<InvestorMasterCams> findByUser_idAndClient_nameNew(Integer userId, String clientName);

    List<InvestorMasterCams> findData(Integer userId, String clientName);
}
