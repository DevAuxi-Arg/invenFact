package com.willysoft.productosapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productosApiOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí el accessToken obtenido en POST /api/auth/login")))
                .info(new Info()
                        .title("Productos API")
                        .description("""
                                API REST de gestión comercial: productos, categorías, usuarios, clientes,
                                facturación (con IVA por categoría y multimoneda ARS/USD) y reportes de ventas.

                                **Autenticación**: la mayoría de los endpoints requieren un JWT.
                                1. Obtené el token en `POST /api/auth/login` (admin inicial: admin@willysoft.com / Admin123!).
                                2. Pulsá **Authorize** (candado) y pegá el `accessToken`.

                                **Roles**: ADMIN (gobierna usuarios y sistema), COADMIN (opera el negocio y da de alta BACKOFFICE) \
                                y BACKOFFICE (operativo de catálogo, clientes y facturación).""")
                        .version("v3.0.0")
                        .contact(new Contact()
                                .name("Willysoft")
                                .email("lic.gfescobar@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
