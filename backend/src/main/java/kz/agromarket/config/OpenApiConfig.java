package kz.agromarket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger (OpenAPI) құжаттамасы.
 * "bearerAuth" схемасы Swagger бетінде «Authorize» батырмасын қосады:
 * кіргеннен кейін алған токенді сонда қойсаң, қорғалған эндпоинттерді тексеруге болады.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agroMarketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgroMarket API")
                        .version("0.2.0")
                        .description("Фермерлердің агромаркетплейсі — Team 1. "
                                + "Кіру: POST /api/auth/login → token → «Authorize» батырмасы."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
