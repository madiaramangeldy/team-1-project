package kz.agromarket.security;

import java.util.List;
import kz.agromarket.auth.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Жүйеге кірген пайдаланушы (JWT токеннен алынады).
 * Контроллерде `@AuthenticationPrincipal AuthUser user` арқылы алынады.
 */
public record AuthUser(Long id, String email, Role role) {

    /** Spring Security рөлдерді "ROLE_" префиксімен күтеді: hasRole("FARMER") → ROLE_FARMER */
    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
