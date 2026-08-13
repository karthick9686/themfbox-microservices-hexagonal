package com.hexagonal.portfolio.application.port.in;

import com.hexagonal.portfolio.domain.model.InvestorPortfolioNewMobileResponse;
import com.hexagonal.portfolio.domain.model.InvestorPortfolioResponse;

/** Primary port: condenses a computed portfolio into the mobile payload shape. */
public interface ConvertToMobilePortfolioUseCase {

    InvestorPortfolioNewMobileResponse convert(InvestorPortfolioResponse source);
}
