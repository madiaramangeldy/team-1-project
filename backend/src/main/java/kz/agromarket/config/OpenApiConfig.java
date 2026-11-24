package kz.agromarket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger (OpenAPI) құжаттамасының тақырыбы мен сипаттамасы. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agroMarketOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AgroMarket API")
                .version("0.1.0")
                .description("Фермерлердің агромаркетплейсі — Team 1. "
                        + "Фермерлер өнімін сатып алушыларға, кафе мен дүкендерге делдалсыз сатады."));
    }
}
