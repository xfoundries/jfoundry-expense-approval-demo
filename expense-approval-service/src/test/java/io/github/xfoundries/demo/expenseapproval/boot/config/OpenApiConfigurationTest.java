package io.github.xfoundries.demo.expenseapproval.boot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigurationTest {

    @Test
    void describesTheOnionArchitectureVariant() {
        OpenAPIDefinition definition = OpenApiConfiguration.class.getAnnotation(OpenAPIDefinition.class);

        assertThat(definition.info().description()).contains("Onion Simple Architecture");
    }
}
