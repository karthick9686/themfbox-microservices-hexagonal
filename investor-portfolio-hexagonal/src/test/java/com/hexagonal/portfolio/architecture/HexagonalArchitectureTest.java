package com.hexagonal.portfolio.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture fitness test for the hexagonal (ports and adapters) layering.
 *
 * <p>Until this existed, the layering held by convention only: nothing failed the build when a
 * future change pointed the domain at Spring, or let an application service reach past its ports
 * straight into a JPA repository. These rules make the dependency direction
 * {@code adapter -> application -> domain} an enforced property rather than a documented intent.
 *
 * <p><strong>Two deliberate exemptions</strong>, both tied to decisions the team has not yet
 * settled. They are encoded here rather than left implicit so that resolving either one is a
 * visible edit to this file:
 *
 * <ol>
 *   <li><strong>Jackson annotations in {@code domain/model}.</strong> This service serialises its
 *       domain read model straight to HTTP with no DTO boundary, so {@code @JsonProperty} and
 *       {@code @JsonIgnore} appear on five domain payload classes. That is the direct cost of the
 *       no-DTO tradeoff. If a DTO layer is introduced, add {@code com.fasterxml.jackson..} to
 *       {@link #FRAMEWORK_PACKAGES} and the rule will hold the line.
 *   <li><strong>Hibernate internals in {@code application/service}.</strong> Eleven call sites in
 *       {@code InvestorPortfolioService} and {@code InvestorTaxReportService} use
 *       {@code org.hibernate.internal.util.StringHelper}. The application layer is therefore
 *       checked against persistence, web and Spring Data types (what the README claims of it)
 *       rather than against all frameworks. Those call sites sit inside 4,700 lines of untested
 *       valuation logic, so they are left alone here; clearing them is a separate, test-first job.
 * </ol>
 */
@DisplayName("Hexagonal architecture")
class HexagonalArchitectureTest {

    private static final String ROOT = "com.hexagonal.portfolio";

    private static final String DOMAIN = ROOT + ".domain..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String ADAPTER = ROOT + ".adapter..";
    private static final String CONFIG = ROOT + ".config..";

    private static final String PORT_IN = ROOT + ".application.port.in";
    private static final String PORT_OUT = ROOT + ".application.port.out";
    private static final String APPLICATION_SERVICE = ROOT + ".application.service";

    private static final String ADAPTER_IN = ROOT + ".adapter.in..";
    private static final String ADAPTER_OUT = ROOT + ".adapter.out..";
    private static final String PERSISTENCE = ROOT + ".adapter.out.persistence";
    private static final String ENTITIES = ROOT + ".adapter.out.persistence.entity..";
    private static final String REPOSITORIES = ROOT + ".adapter.out.persistence.repository..";

    /**
     * Frameworks the domain must stay clear of. Jackson is deliberately absent — see the class
     * javadoc. {@code org.hibernate..} is included: ORM internals are as much an infrastructure
     * leak as {@code jakarta.persistence}, and arguably worse, being implementation detail.
     */
    private static final String[] FRAMEWORK_PACKAGES = {
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.servlet..",
            "org.hibernate..",
    };

    /** Infrastructure the application layer must reach only through its output ports. */
    private static final String[] INFRASTRUCTURE_PACKAGES = {
            "jakarta.persistence..",
            "jakarta.servlet..",
            "org.springframework.data..",
            "org.springframework.web..",
    };

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Nested
    @DisplayName("dependency rule")
    class DependencyRule {

        @Test
        @DisplayName("domain depends on nothing else in the service")
        void domainIsSelfContained() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER, CONFIG)
                    .because("the domain sits at the centre and must not know its callers");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("application never reaches into an adapter")
        void applicationDoesNotDependOnAdapters() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(ADAPTER, CONFIG)
                    .because("adapters are plugged into the application through ports, not called by it");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("the driving adapter does not know the driven adapter")
        void adaptersDoNotDependOnEachOther() {
            ArchRule webDoesNotSeePersistence = noClasses()
                    .that().resideInAPackage(ADAPTER_IN)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER_OUT)
                    .because("the web adapter drives use cases, not repositories");

            ArchRule persistenceDoesNotSeeWeb = noClasses()
                    .that().resideInAPackage(ADAPTER_OUT)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER_IN)
                    .because("a driven adapter has no business knowing what drives it");

            webDoesNotSeePersistence.check(productionClasses);
            persistenceDoesNotSeeWeb.check(productionClasses);
        }
    }

    @Nested
    @DisplayName("framework isolation")
    class FrameworkIsolation {

        @Test
        @DisplayName("domain is free of Spring, JPA, Hibernate and Servlet types")
        void domainIsFrameworkFree() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
                    .because("domain rules must be testable and portable without a container");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("application is free of persistence and web types")
        void applicationIsFreeOfInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE_PACKAGES)
                    .because("use cases speak in ports, not in entities, repositories or HTTP");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("JPA entities and repositories stay inside the persistence adapter")
        void persistenceTypesDoNotEscape() {
            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackage(ADAPTER_OUT)
                    .should().dependOnClassesThat().resideInAnyPackage(ENTITIES, REPOSITORIES)
                    .because("entities are a storage detail; the rest of the service uses domain models");

            rule.check(productionClasses);
        }
    }

    @Nested
    @DisplayName("port and adapter conventions")
    class Conventions {

        @Test
        @DisplayName("inbound ports are interfaces named *UseCase")
        void inboundPortsAreNamedUseCase() {
            ArchRule rule = classes()
                    .that().resideInAPackage(PORT_IN)
                    .should().beInterfaces()
                    .andShould().haveSimpleNameEndingWith("UseCase");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("outbound ports are interfaces named *Port")
        void outboundPortsAreNamedPort() {
            ArchRule rule = classes()
                    .that().resideInAPackage(PORT_OUT)
                    .should().beInterfaces()
                    .andShould().haveSimpleNameEndingWith("Port");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("persistence adapters are named *Adapter")
        void persistenceAdaptersAreNamedAdapter() {
            ArchRule rule = classes()
                    .that().resideInAPackage(PERSISTENCE)
                    .and().haveSimpleNameNotEndingWith("Mapper")
                    .should().haveSimpleNameEndingWith("Adapter")
                    .because("the driven side of a port is an adapter and should read as one");

            rule.check(productionClasses);
        }

        /**
         * Only the ports are public API. Keeping the implementations package-private is what stops
         * a caller from binding to {@code InvestorPortfolioService} instead of
         * {@code GetInvestorPortfolioUseCase} and quietly reintroducing the coupling the ports exist
         * to prevent. See {@code ApplicationServiceProxyTest} for the proxying this relies on.
         */
        @Test
        @DisplayName("use-case implementations are package-private; only ports are public")
        void useCaseImplementationsArePackagePrivate() {
            ArchRule rule = classes()
                    .that().resideInAPackage(APPLICATION_SERVICE)
                    .should().bePackagePrivate()
                    .because("the port is the published contract, not the class behind it");

            rule.check(productionClasses);
        }

        @Test
        @DisplayName("use cases are implemented only in application/service")
        void useCasesAreImplementedInTheApplicationLayer() {
            ArchRule rule = classes()
                    .that().areNotInterfaces()
                    .and().implement(com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage(PORT_IN))
                    .should().resideInAPackage(APPLICATION_SERVICE)
                    .because("an inbound port is fulfilled by a use-case implementation, nowhere else");

            rule.check(productionClasses);
        }
    }
}
