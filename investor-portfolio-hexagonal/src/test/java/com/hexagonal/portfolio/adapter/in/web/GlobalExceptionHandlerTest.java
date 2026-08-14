package com.hexagonal.portfolio.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.hexagonal.portfolio.domain.exception.InvalidRequestException;
import com.hexagonal.portfolio.domain.exception.PortfolioNotFoundException;

/**
 * Direct tests for {@link GlobalExceptionHandler}, covering the cases the controller tests cannot
 * reach through {@code MockMvc}.
 *
 * <p>The first two exist because of a real regression. Spring MVC signals its own client errors as
 * exceptions — an unknown URL is a {@code NoResourceFoundException}, a wrong verb is a
 * {@code HttpRequestMethodNotSupportedException} — and the catch-all handler originally swallowed
 * them, so a browser asking for {@code /favicon.ico} got a 500 and logged a full stack trace at
 * ERROR. Anything watching 5xx rates would have seen a permanently unhealthy service.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("an unknown path is 404, not a 500")
    void unknownPathIsNotFound() {
        ProblemDetail problem = handler.handleUnexpected(
                new NoResourceFoundException(HttpMethod.GET, "favicon.ico"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getProperties()).containsEntry("code", "PORTFOLIO-4041");
    }

    @Test
    @DisplayName("an unsupported HTTP method keeps its own 405 status")
    void unsupportedMethodKeepsItsStatus() {
        ProblemDetail problem = handler.handleUnexpected(
                new HttpRequestMethodNotSupportedException("POST"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @Test
    @DisplayName("a genuine bug is still a 500 with an opaque body")
    void genuineBugIsInternalServerError() {
        ProblemDetail problem = handler.handleUnexpected(
                new IllegalStateException("connection pool exhausted at 10.0.0.7"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getProperties()).containsEntry("code", "PORTFOLIO-5000");
        assertThat(problem.getDetail()).doesNotContain("connection pool", "10.0.0.7");
    }

    @Test
    @DisplayName("a deliberate invalid-request failure is a 400")
    void invalidRequestIsBadRequest() {
        ProblemDetail problem = handler.handlePortfolioException(
                new InvalidRequestException("userId must be a numeric investor id"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "PORTFOLIO-4001");
        assertThat(problem.getDetail()).isEqualTo("userId must be a numeric investor id");
    }

    @Test
    @DisplayName("a missing portfolio is a 404 distinct from a missing resource")
    void missingPortfolioIsNotFound() {
        ProblemDetail problem = handler.handlePortfolioException(
                new PortfolioNotFoundException("no portfolio for investor 42"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getProperties()).containsEntry("code", "PORTFOLIO-4040");
    }

    @Test
    @DisplayName("every response carries a stable code and a timestamp")
    void everyProblemCarriesCodeAndTimestamp() {
        ProblemDetail problem = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(problem.getProperties()).containsKey("code");
        assertThat(problem.getProperties()).containsKey("timestamp");
        assertThat(problem.getTitle()).isNotBlank();
        assertThat(problem.getType()).isNotNull();
    }
}
