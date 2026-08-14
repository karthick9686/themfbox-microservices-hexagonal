package com.hexagonal.portfolio.adapter.in.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hexagonal.portfolio.domain.exception.ErrorCode;
import com.hexagonal.portfolio.domain.exception.PortfolioException;

import lombok.extern.slf4j.Slf4j;

/**
 * Single translation point from exception to HTTP response, in RFC 7807 {@link ProblemDetail} form.
 *
 * <p>Replaces the previous arrangement, where {@code getInvestorPortfolioNew} wrapped its whole body
 * in {@code catch (Exception e) { printStackTrace(); return badRequest(); }} and
 * {@code getTaxReportsNew} caught nothing at all. That had two consequences worth stating, because
 * both are now fixed:
 *
 * <ul>
 *   <li>A genuine bug — a null dereference in the valuation, a dead datasource — was reported to the
 *       caller as <em>400 Bad Request</em>, blaming the client for a server fault and hiding the
 *       failure from any monitoring that watches 5xx rates.
 *   <li>The two endpoints disagreed: the same internal fault produced a 400 on one and a bare 500 on
 *       the other.
 * </ul>
 *
 * <p>Now: anticipated failures carry their {@link ErrorCode} and map to a 4xx; everything else is an
 * {@link ErrorCode#INTERNAL_ERROR} 500, logged in full server-side and returned to the caller
 * without stack trace, exception class, or message. Callers should branch on the {@code code}
 * property, which is stable, not on {@code detail}, which is prose.
 *
 * <p><strong>This diverges from the legacy service on purpose.</strong> The port was otherwise built
 * for byte-for-byte response equivalence, and error payloads no longer match. Success payloads are
 * untouched. When the outstanding legacy response-diff is run, the failure paths need reviewing
 * rather than diffing.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI VALIDATION_TYPE = URI.create("urn:mfbox:portfolio:validation-failed");
    private static final URI INVALID_PARAM_TYPE = URI.create("urn:mfbox:portfolio:invalid-parameter");
    private static final URI NOT_FOUND_TYPE = URI.create("urn:mfbox:portfolio:not-found");
    private static final URI RESOURCE_NOT_FOUND_TYPE = URI.create("urn:mfbox:portfolio:resource-not-found");
    private static final URI INTERNAL_TYPE = URI.create("urn:mfbox:portfolio:internal-error");

    /** Failures the service raised deliberately, carrying their own code. */
    @ExceptionHandler(PortfolioException.class)
    public ProblemDetail handlePortfolioException(PortfolioException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = statusFor(code);

        if (status.is5xxServerError()) {
            log.error("Portfolio failure [{}]", code.getCode(), ex);
        } else {
            log.warn("Rejected request [{}]: {}", code.getCode(), ex.getMessage());
        }

        return problem(status, code, ex.getMessage());
    }

    /** A required {@code @RequestParam} was absent. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter '{}'", ex.getParameterName());

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Required parameter '" + ex.getParameterName() + "' is missing");
        problem.setProperty("parameter", ex.getParameterName());
        return problem;
    }

    /** A parameter was present but could not be bound to its declared type. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter '{}' could not be bound to {}", ex.getName(),
                ex.getRequiredType() == null ? "the expected type" : ex.getRequiredType().getSimpleName());

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                "Parameter '" + ex.getName() + "' is not of the expected type");
        problem.setProperty("parameter", ex.getName());
        return problem;
    }

    /**
     * Bean Validation failures on controller method parameters. Spring Framework 6.1 raises
     * {@code HandlerMethodValidationException} for constrained {@code @RequestParam}s.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> violations = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName()
                                + " " + error.getDefaultMessage()))
                .toList();

        log.warn("Request rejected by validation: {}", violations);

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.getDefaultMessage());
        problem.setProperty("violations", violations);
        return problem;
    }

    /** The same, when validation is driven through the proxy-based path instead. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<String> violations = ex.getConstraintViolations().stream()
                .map(this::describe)
                .toList();

        log.warn("Request rejected by validation: {}", violations);

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.getDefaultMessage());
        problem.setProperty("violations", violations);
        return problem;
    }

    /**
     * Anything unanticipated. Logged in full, reported opaquely — no stack trace, no exception
     * class, no message reaches the caller.
     *
     * <p>Spring MVC raises its own client-error signals as exceptions — an unknown URL becomes
     * {@code NoResourceFoundException}, a wrong verb becomes
     * {@code HttpRequestMethodNotSupportedException}. Those all implement {@link ErrorResponse} and
     * already carry the right status, so they are honoured rather than flattened into a 500. Without
     * this check a request for {@code /favicon.ico} answers 500 and logs a full stack trace at
     * ERROR, which is both wrong for the caller and corrosive to any alerting that watches 5xx
     * rates. Testing {@code instanceof} here rather than adding a handler per type keeps future
     * Spring signals covered too — {@code ErrorResponse} is not a {@code Throwable}, so it cannot
     * be an {@code @ExceptionHandler} target in its own right.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            if (status.is4xxClientError()) {
                ErrorCode code = status == HttpStatus.NOT_FOUND
                        ? ErrorCode.RESOURCE_NOT_FOUND
                        : ErrorCode.VALIDATION_FAILED;
                log.debug("Request rejected by Spring MVC [{}]: {}", code.getCode(), ex.getMessage());
                return problem(status, code, code.getDefaultMessage());
            }
        }

        log.error("Unhandled failure serving request", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }

    private String describe(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        String parameter = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return parameter + " " + violation.getMessage();
    }

    private HttpStatus statusFor(ErrorCode code) {
        return switch (code) {
            case VALIDATION_FAILED, INVALID_PARAMETER -> HttpStatus.BAD_REQUEST;
            case PORTFOLIO_NOT_FOUND, RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ProblemDetail problem(HttpStatus status, ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, detail == null ? code.getDefaultMessage() : detail);
        problem.setTitle(titleFor(code));
        problem.setType(typeFor(code));
        problem.setProperty("code", code.getCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private String titleFor(ErrorCode code) {
        return switch (code) {
            case VALIDATION_FAILED -> "Validation failed";
            case INVALID_PARAMETER -> "Invalid parameter";
            case PORTFOLIO_NOT_FOUND -> "Portfolio not found";
            case RESOURCE_NOT_FOUND -> "Resource not found";
            case INTERNAL_ERROR -> "Internal error";
        };
    }

    private URI typeFor(ErrorCode code) {
        return switch (code) {
            case VALIDATION_FAILED -> VALIDATION_TYPE;
            case INVALID_PARAMETER -> INVALID_PARAM_TYPE;
            case PORTFOLIO_NOT_FOUND -> NOT_FOUND_TYPE;
            case RESOURCE_NOT_FOUND -> RESOURCE_NOT_FOUND_TYPE;
            case INTERNAL_ERROR -> INTERNAL_TYPE;
        };
    }
}
