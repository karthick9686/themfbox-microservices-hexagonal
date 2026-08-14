package com.hexagonal.portfolio.config;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails startup with a readable message when a required credential is missing.
 *
 * <p>Without this the failure is real but the diagnostic is not. Datasource settings are bound
 * through {@code @ConfigurationProperties}, and that binder leaves an unresolvable
 * {@code ${PLACEHOLDER}} as literal text rather than throwing. The application therefore hands the
 * string {@code ${DB_PRIMARY_USERNAME}} to MySQL as a username and dies several seconds later
 * with:
 *
 * <pre>Access denied for user '${DB_PRIMARY_USERNAME}'@'...' (using password: YES)</pre>
 *
 * <p>which reads like a credentials problem rather than a missing-configuration problem, and only
 * surfaces after Tomcat and both EntityManagerFactories have started. Checking here — in an
 * {@link EnvironmentPostProcessor}, before any bean is created — turns that into an immediate,
 * self-explanatory error naming the variables that are absent.
 */
public class RequiredEnvironmentValidator implements EnvironmentPostProcessor {

    /**
     * The NAMES of the environment variables that must be present — never their values.
     *
     * <p>Each entry is looked up with {@code environment.getProperty(name)}. Putting an actual
     * username or password here would do two harmful things: the application would search for an
     * environment variable whose name is the password (which cannot exist, so startup always
     * fails), and the secret would be hardcoded into a file that is committed to git. The values
     * belong in {@code .env}, which is gitignored.
     */
    private static final List<String> REQUIRED = List.of(
            "DB_PRIMARY_USERNAME",
            "DB_PRIMARY_PASSWORD",
            "DB_SECONDARY_USERNAME",
            "DB_SECONDARY_PASSWORD");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> missing = REQUIRED.stream()
                .filter(key -> isBlank(environment.getProperty(key)))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(buildMessage(missing));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildMessage(List<String> missing) {
        return """

                =====================================================================
                Missing required configuration: %s

                These are database credentials and are deliberately not committed —
                the values that used to live in application-*.properties were pushed
                to a public repository and must be treated as compromised.

                Supply them in either way:

                  1. A local .env file next to the pom (gitignored):
                         cp .env.example .env      then fill it in

                  2. Environment variables / JVM args:
                         -DDB_PRIMARY_USERNAME=... -DDB_PRIMARY_PASSWORD=...

                See .env.example for the full list.
                =====================================================================
                """.formatted(String.join(", ", missing));
    }
}
