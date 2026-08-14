package com.hexagonal.portfolio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class HexagonalPortfolioApplication
{
    @Value("${custom.server.url}")
    private String serverUrl;

    public static void main(String[] args)
    {
        var context = SpringApplication.run(HexagonalPortfolioApplication.class, args);
        ConfigurableEnvironment env = (ConfigurableEnvironment) context.getEnvironment();
        String activeProfile = env.getActiveProfiles().length > 0 ? env.getActiveProfiles()[0] : "default";

        if (activeProfile.equalsIgnoreCase("prod"))
        {
            log.info("Hexagonal Portfolio Services Production Server started!");
        } else if (activeProfile.equalsIgnoreCase("dev"))
        {
            log.info("Hexagonal Portfolio Services Development Server started!");
        } else
        {
            log.info("Hexagonal Portfolio Services {} Server started!", activeProfile);
        }
    }
}
