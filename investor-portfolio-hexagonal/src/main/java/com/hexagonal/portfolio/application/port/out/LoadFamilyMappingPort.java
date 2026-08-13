package com.hexagonal.portfolio.application.port.out;

import com.hexagonal.portfolio.domain.model.UsersMapping;

import java.util.List;

/** Resolves the investors mapped to a family head. */
public interface LoadFamilyMappingPort {

    List<UsersMapping> getFamilyMappedInvestor(Integer familyHeadId, String clientName);
}
