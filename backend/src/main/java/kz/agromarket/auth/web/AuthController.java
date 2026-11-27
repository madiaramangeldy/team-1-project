package kz.agromarket.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.agromarket.auth.application.AccountService;
import kz.agromarket.auth.web.AuthDtos.LoginRequest;
import kz.agromarket.auth.web.AuthDtos.LoginResponse;
import kz.agromarket.auth.web.AuthDtos.ProfileRequest;
import kz.agromarket.auth.web.AuthDtos.SignUpRequest;
import kz.agromarket.auth.web.AuthDtos.UserResponse;
import kz.agromarket.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-01: тіркелу, кіру, ағымдағы пайдаланушы. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Тіркелу және кіру (US-01)")
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @Operation(summary = "Тіркелу (BUYER, FARMER немесе COURIER). Фермерге regionId міндетті")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Кіру: JWT токен қайтарады")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return accounts.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Ағымдағы пайдаланушы (токен керек)")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return accounts.me(user.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Профильді өзгерту: аты, телефон, (фермерге) шаруашылық атауы")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse updateMe(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ProfileRequest request) {
        return accounts.updateProfile(user.id(), request);
    }
}
