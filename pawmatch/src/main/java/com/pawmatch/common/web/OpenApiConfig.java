package com.pawmatch.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

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

    /**
     * Merges the hand-written components from {@code resources/openapi/*.yaml} (schemas, request bodies, examples)
     * into the generated spec. Files are read once here, so a broken file fails application startup.
     */
    @Bean
    OpenApiCustomizer componentFilesCustomizer(ResourcePatternResolver resourcePatternResolver) {
        Components components = OpenApiComponentFiles.load(resourcePatternResolver);
        return openApi -> OpenApiComponentFiles.mergeInto(openApi, components);
    }
}
