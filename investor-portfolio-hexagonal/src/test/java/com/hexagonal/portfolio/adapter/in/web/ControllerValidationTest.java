package com.hexagonal.portfolio.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import com.hexagonal.portfolio.application.port.in.ConvertToCapitalGainReportUseCase;
import com.hexagonal.portfolio.application.port.in.ConvertToMobilePortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetFamilyMfPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetFolioMasterSummaryUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorPortfolioUseCase;
import com.hexagonal.portfolio.application.port.in.GetInvestorTaxReportUseCase;

/**
 * Proves the parameter constraints on {@link InvestorPortfolioController} are load-bearing.
 *
 * <p>Worth its own test class, because the controller tests cannot show this. A standalone
 * {@code MockMvc} setup has no {@link MethodValidationPostProcessor}, so {@code @NotBlank} and
 * {@code @Pattern} never fire there — those tests pass on the controller's own parse backstop
 * instead, and would go on passing if every constraint annotation were deleted.
 *
 * <p>So this drives the controller through the same proxy Spring Boot applies at runtime to a
 * {@code @Validated} bean, and asserts the constraints reject before the method body runs — then
 * checks {@link GlobalExceptionHandler} turns that rejection into a 400 rather than a 500.
 */
@DisplayName("controller parameter validation")
class ControllerValidationTest {

    private GetInvestorPortfolioUseCase getInvestorPortfolioUseCase;
    private GetFamilyMfPortfolioUseCase getFamilyMfPortfolioUseCase;
    private GetFolioMasterSummaryUseCase getFolioMasterSummaryUseCase;
    private ConvertToMobilePortfolioUseCase convertToMobilePortfolioUseCase;
    private GetInvestorTaxReportUseCase getInvestorTaxReportUseCase;
    private ConvertToCapitalGainReportUseCase convertToCapitalGainReportUseCase;

    private InvestorPortfolioController validatingController;

    @BeforeEach
    void setUp() {
        getInvestorPortfolioUseCase = mock(GetInvestorPortfolioUseCase.class);
        getFamilyMfPortfolioUseCase = mock(GetFamilyMfPortfolioUseCase.class);
        getFolioMasterSummaryUseCase = mock(GetFolioMasterSummaryUseCase.class);
        convertToMobilePortfolioUseCase = mock(ConvertToMobilePortfolioUseCase.class);
        getInvestorTaxReportUseCase = mock(GetInvestorTaxReportUseCase.class);
        convertToCapitalGainReportUseCase = mock(ConvertToCapitalGainReportUseCase.class);

        InvestorPortfolioController controller = new InvestorPortfolioController(
                getInvestorPortfolioUseCase,
                getFamilyMfPortfolioUseCase,
                getFolioMasterSummaryUseCase,
                convertToMobilePortfolioUseCase,
                getInvestorTaxReportUseCase,
                convertToCapitalGainReportUseCase);

        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.afterPropertiesSet();
        validatingController = (InvestorPortfolioController)
                processor.postProcessAfterInitialization(controller, "investorPortfolioController");
    }

    @ParameterizedTest(name = "userId=\"{0}\" is rejected before the use case is called")
    @ValueSource(strings = {"", "   ", "abc", "4.2", "-1", "12x"})
    @DisplayName("a userId that is blank or non-numeric violates a declared constraint")
    void rejectsInvalidUserId(String userId) {
        assertThatThrownBy(() -> validatingController.getInvestorPortfolioNew(
                userId, "acme", null, null, null, null, null, null))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(getInvestorPortfolioUseCase, getFamilyMfPortfolioUseCase);
    }

    @ParameterizedTest(name = "userId=\"{0}\" passes validation")
    @ValueSource(strings = {"42", "  42  ", "0", "999999"})
    @DisplayName("a numeric userId satisfies the constraint, padding included")
    void acceptsNumericUserId(String userId) {
        assertThatCode(() -> validatingController.getInvestorPortfolioNew(
                userId, "acme", null, null, null, null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a blank required parameter on the tax endpoint is rejected too")
    void rejectsBlankTaxParameters() {
        assertThatThrownBy(() -> validatingController.getTaxReportsNew(
                42, "acme", "  ", "All", "", "", "web"))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(getInvestorTaxReportUseCase);
    }

    @Test
    @DisplayName("the handler renders a constraint violation as 400, not 500")
    void handlerMapsViolationToBadRequest() {
        ConstraintViolationException violation = catchViolation();

        ProblemDetail problem = new GlobalExceptionHandler().handleConstraintViolation(violation);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "PORTFOLIO-4000");
        assertThat(problem.getProperties().get("violations").toString()).contains("userId");
    }

    private ConstraintViolationException catchViolation() {
        try {
            validatingController.getInvestorPortfolioNew(
                    "not-a-number", "acme", null, null, null, null, null, null);
            throw new AssertionError("expected the constraint to be violated");
        } catch (ConstraintViolationException expected) {
            return expected;
        }
    }
}
