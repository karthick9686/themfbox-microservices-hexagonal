package com.hexagonal.portfolio.domain.exception;

/**
 * Base type for every failure this service raises deliberately.
 *
 * <p>The distinction that matters: a {@code PortfolioException} is an outcome the service
 * anticipated and can describe to the caller. Anything else reaching
 * {@code GlobalExceptionHandler} is a bug, and is reported as an opaque
 * {@link ErrorCode#INTERNAL_ERROR} rather than dressed up as a client error.
 */
public abstract class PortfolioException extends RuntimeException {

    private final ErrorCode errorCode;

    protected PortfolioException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected PortfolioException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
