package com.pawmatch.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI pawmatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PawMatch API")
                .version("v1")
                .description("Dog breed recommendations based on a lifestyle questionnaire. "
                        + "The compatibility index (0–100) is an index, not a probability of success."));
    }
}
