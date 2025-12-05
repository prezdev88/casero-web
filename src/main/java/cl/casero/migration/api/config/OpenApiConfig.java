package cl.casero.migration.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI caseroOpenApi() {
        return new OpenAPI()
            .openapi("3.0.1")
            .info(new Info()
                .title("Casero API")
                .version("v1")
                .description("Capas REST para las vistas actuales de Casero."));
    }

    @Bean
    public GroupedOpenApi caseroApiGroup() {
        return GroupedOpenApi.builder()
            .group("casero-api")
            .pathsToMatch("/api/**")
            .build();
    }
}
