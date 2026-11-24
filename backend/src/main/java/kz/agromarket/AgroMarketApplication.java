package kz.agromarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot қосымшасының кіру нүктесі.
 *
 * Іске қосу: VS Code-та осы файлдағы «Run» немесе `mvn spring-boot:run`.
 * Swagger: http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AgroMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgroMarketApplication.class, args);
    }
}
