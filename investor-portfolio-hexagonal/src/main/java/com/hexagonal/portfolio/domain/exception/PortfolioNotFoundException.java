package com.hexagonal.portfolio.domain.exception;

/**
 * No portfolio exists for the requested investor.
 *
 * <p>Not currently thrown: the ported valuation returns an empty response rather than signalling
 * absence, and changing that would alter the payload. It exists so that the taxonomy has a
 * not-found case to grow into once the legacy response-diff has been run and the emptiness
 * behaviour can be revisited deliberately.
 */
public class PortfolioNotFoundException extends PortfolioException {

    public PortfolioNotFoundException(String message) {
        super(ErrorCode.PORTFOLIO_NOT_FOUND, message);
    }
}
