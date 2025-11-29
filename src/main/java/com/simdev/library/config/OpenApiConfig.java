package com.simdev.library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI (Swagger) pour la documentation de l'API.
 * Configure la documentation Swagger avec les informations de l'API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryManagementOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Server URL in Development environment");

        Contact contact = new Contact();
        contact.setEmail("simdev@example.com");
        contact.setName("Library Management API");
        contact.setUrl("https://github.com/simdev/library-management");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Library Management API")
                .version("1.0.0")
                .contact(contact)
                .description("API RESTful pour la gestion d'une bibliothèque avec Spring Boot, JPA, HATEOAS et Swagger")
                .termsOfService("https://example.com/terms")
                .license(mitLicense);

        SecurityScheme basicAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic");

        return new OpenAPI()
                .info(info)
                .servers(List.of(server))
                .components(new Components().addSecuritySchemes("basicAuth", basicAuth))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }

    /**
     * Configuration pour grouper les APIs par domaine.
     * Cela permet d'organiser la documentation Swagger.
     * Utilise displayName pour éviter les problèmes de sérialisation.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("library-management")
                .displayName("Library Management API")
                .pathsToMatch("/api/**")
                .packagesToScan("com.simdev.library.controller")
                .build();
    }
}
