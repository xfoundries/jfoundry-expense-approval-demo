package io.github.xfoundries.demo.expenseapproval.boot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "团队费用报销审批 API",
        version = "v1",
        description = "用于验证领域建模、Onion Simple Architecture 和 jfoundry 落地的完整 Demo"))
public class OpenApiConfiguration {
}
