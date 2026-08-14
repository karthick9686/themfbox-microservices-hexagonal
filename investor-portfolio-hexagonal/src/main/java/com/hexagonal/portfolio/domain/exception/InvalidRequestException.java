package com.hexagonal.portfolio.domain.exception;

/**
 * A request the service understood but cannot act on — a non-numeric investor id, a date range
 * that does not parse, and so on. Always the caller's fault, never a bug.
 */
public class InvalidRequestException extends PortfolioException {

    public InvalidRequestException(String message) {
        super(ErrorCode.INVALID_PARAMETER, message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(ErrorCode.INVALID_PARAMETER, message, cause);
    }
}
