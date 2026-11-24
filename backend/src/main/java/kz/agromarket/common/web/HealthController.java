package kz.agromarket.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Қосымшаның жұмыс істеп тұрғанын тексеру.
 * Docker Compose және CI осы эндпоинт арқылы backend-тің іске қосылғанын біледі.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "System", description = "Жүйенің күйі")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Backend жұмыс істеп тұр ма")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "agromarket-backend",
                "version", "0.1.0",
                "time", Instant.now().toString());
    }
}
