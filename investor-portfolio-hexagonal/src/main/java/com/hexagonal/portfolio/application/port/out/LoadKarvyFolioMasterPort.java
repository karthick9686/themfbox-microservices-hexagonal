package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.InvestorMasterKarvy;

import java.util.List;

/** Loads Karvy folio master records (demat flags, KYC and nominee details). */
public interface LoadKarvyFolioMasterPort {

    List<InvestorMasterKarvy> findByUser_idAndClient_nameNew(Integer userId, String clientName);

    List<InvestorMasterKarvy> findByUserIdAndClientNameOrderByIdDesc(Integer userId, String clientName);
}
