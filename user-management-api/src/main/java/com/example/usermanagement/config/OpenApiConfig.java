package com.example.usermanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("User Management REST API")
                .version("v1")
                .description("REST API per la gestione utenti con RBAC Keycloak e filtering per ruolo"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }

    /**
     * Sovrascrive il parametro "sort" generato da SpringDoc per Pageable,
     * impostando un esempio significativo al posto del default "string".
     */
    @Bean
    public OperationCustomizer pageableSortCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) return operation;
            operation.getParameters().stream()
                .filter(p -> "sort".equals(p.getName()))
                .forEach(p -> p.schema(new StringSchema()
                    .example("createdAt,desc")
                    ._default("createdAt,desc"))
                    .description("Ordinamento: campo,direzione — es. createdAt,desc oppure username,asc")
                );
            return operation;
        };
    }
}
