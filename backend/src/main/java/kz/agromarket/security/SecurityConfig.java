package kz.agromarket.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import kz.agromarket.common.web.ApiError;
import kz.agromarket.config.AgroMarketProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Қауіпсіздік баптаулары: қай эндпоинтке кім кіре алады.
 *
 * <ul>
 *   <li>Ашық: тіркелу, кіру, health, Swagger, каталогты қарау (GET)</li>
 *   <li>FARMER: өнім қосу, өзгерту, жою</li>
 *   <li>MODERATOR: фермерлерді верификациялау</li>
 *   <li>Қалғаны: кез келген жүйеге кірген пайдаланушы</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC = {
        "/api/health", "/api/auth/register", "/api/auth/login", "/error",
        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    /** Құпиясөздерді BCrypt-пен хэштейміз: әр хэште кездейсоқ salt бар, кері шешу мүмкін емес */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtService jwtService(AgroMarketProperties properties) {
        return new JwtService(properties.jwt().secret(), properties.jwt().ttlMinutes());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper)
            throws Exception {
        http
                // REST API + JWT: cookie-сессия жоқ, сондықтан CSRF қорғанысы керек емес
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/catalog/**",
                                "/api/files/products/**", "/api/farmers/**").permitAll()
                        .requestMatchers("/api/moderation/**").hasRole("MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("FARMER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("FARMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("FARMER")
                        .requestMatchers("/api/cart/**", "/api/orders/**", "/api/reviews/**").hasRole("BUYER")
                        .requestMatchers("/api/farmer/**").hasRole("FARMER")
                        .requestMatchers("/api/courier/**").hasRole("COURIER")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(objectMapper, request, response, 401, "UNAUTHORIZED", "Жүйеге кіру керек"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(objectMapper, request, response, 403, "FORBIDDEN", "Бұл әрекетке рұқсат жоқ")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 401 және 403 қателері де бірыңғай ApiError форматында қайтады */
    private static void writeError(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                                   int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(code, message, status, request.getRequestURI()));
    }
}
