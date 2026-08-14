package com.hexagonal.portfolio.domain.exception;

/**
 * Typed error taxonomy for the investor-portfolio read model.
 *
 * <p>Deliberately free of HTTP: an {@code ErrorCode} says what went wrong in the language of the
 * domain, and the web adapter decides which status code that maps to. Keeping the mapping in
 * {@code GlobalExceptionHandler} rather than here is what lets the domain stay transport-agnostic
 * — the same rule the architecture fitness test enforces.
 *
 * <p>The {@code code} is the stable identifier clients should branch on. Messages are for humans
 * and may be reworded; codes are not.
 */
public enum ErrorCode {

    /** A request parameter was missing, malformed, or failed a declared constraint. */
    VALIDATION_FAILED("PORTFOLIO-4000", "One or more request parameters are invalid"),

    /** A parameter was well-formed but not usable — e.g. a non-numeric investor id. */
    INVALID_PARAMETER("PORTFOLIO-4001", "A request parameter could not be interpreted"),

    /** No portfolio exists for the requested investor. */
    PORTFOLIO_NOT_FOUND("PORTFOLIO-4040", "No portfolio was found for the given investor"),

    /** The requested path or resource does not exist — an unknown URL, a missing static file. */
    RESOURCE_NOT_FOUND("PORTFOLIO-4041", "The requested resource does not exist"),

    /** Anything unexpected. Never carries internal detail to the caller. */
    INTERNAL_ERROR("PORTFOLIO-5000", "The request could not be completed");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
