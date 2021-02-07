package ru.golovkin.oxford3000.dictionary.config;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("!test")
public class SwaggerConfig {
    private static final Set<String> DEFAULT_PRODUCES_AND_CONSUMES = new HashSet<>(asList("application/json"));

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .produces(DEFAULT_PRODUCES_AND_CONSUMES)
            .consumes(DEFAULT_PRODUCES_AND_CONSUMES)
            .select()
            .apis(basePackage("ru.golovkin.oxford3000"))
            .apis(not(basePackage("ru.golovkin.oxford3000.config")))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
            "Oxford 3000 API",
            "",
            "1.0",
            "",
            new springfox.documentation.service.Contact("Maksim Golovkin", "", "m4ks1k@gmail.com"),
            "All rights reserved. (C) Maksim Golovkin", "",
            emptyList()
        );
    }
}
