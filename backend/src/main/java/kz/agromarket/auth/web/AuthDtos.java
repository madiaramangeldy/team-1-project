package kz.agromarket.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.VerificationStatus;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.auth.persistence.UserEntity;

/**
 * Auth API-дің DTO-лары (Data Transfer Object): клиент пен сервер арасында жүретін JSON.
 * Entity-ді тікелей қайтармаймыз — оның ішінде password_hash бар.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** POST /api/auth/register */
    public record SignUpRequest(
            @NotBlank(message = "Email міндетті") @Email(message = "Email дұрыс емес") String email,
            @NotBlank(message = "Құпиясөз міндетті")
            @Size(min = 8, max = 100, message = "Құпиясөз кемінде 8 таңба") String password,
            @NotBlank(message = "Аты міндетті") @Size(max = 150) String name,
            @Size(max = 30) String phone,
            @NotNull(message = "Рөл міндетті") Role role,
            /* тек FARMER үшін */ Long regionId,
            /* тек FARMER үшін, болмаса аты алынады */ @Size(max = 150) String farmName) {
    }

    /** POST /api/auth/login */
    public record LoginRequest(
            @NotBlank(message = "Email міндетті") @Email(message = "Email дұрыс емес") String email,
            @NotBlank(message = "Құпиясөз міндетті") String password) {
    }

    /** Кіргеннен кейінгі жауап: токен + пайдаланушы */
    public record LoginResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
    }

    /** Пайдаланушы туралы ақпарат (құпиясөзсіз) */
    public record UserResponse(Long id, String email, String name, String phone, Role role,
                               Long farmerProfileId, VerificationStatus verificationStatus,
                               String farmName, String region, String rejectReason, boolean documentUploaded) {

        public static UserResponse of(UserEntity user, FarmerProfileEntity profile) {
            boolean farmer = profile != null;
            return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getPhone(), user.getRole(),
                    farmer ? profile.getId() : null,
                    farmer ? profile.getVerificationStatus() : null,
                    farmer ? profile.getFarmName() : null,
                    farmer ? profile.getRegion().getName() : null,
                    farmer ? profile.getRejectReason() : null,
                    farmer && profile.getDocumentUrl() != null);
        }
    }

    /** PUT /api/auth/me — профильді өзгерту */
    public record ProfileRequest(
            @NotBlank(message = "Аты міндетті") @Size(max = 150) String name,
            @Size(max = 30) String phone,
            @Size(max = 150) String farmName) {
    }

    /** Модераторға көрсетілетін фермер */
    public record FarmerResponse(Long id, Long userId, String email, String name, String farmName,
                                 String region, VerificationStatus status, String documentUrl, String rejectReason) {

        public static FarmerResponse of(FarmerProfileEntity profile) {
            UserEntity user = profile.getUser();
            return new FarmerResponse(profile.getId(), user.getId(), user.getEmail(), user.getName(),
                    profile.getFarmName(), profile.getRegion().getName(), profile.getVerificationStatus(),
                    documentLink(profile), profile.getRejectReason());
        }

        /** Құжаттың өзі емес, оны ашатын сілтеме (тек модераторға рұқсат етілген) */
        private static String documentLink(FarmerProfileEntity profile) {
            return profile.getDocumentUrl() == null ? null
                    : "/api/moderation/farmers/" + profile.getId() + "/document";
        }
    }

    /** POST /api/moderation/farmers/{id}/reject */
    public record RejectRequest(@NotBlank(message = "Бас тарту себебі міндетті") @Size(max = 500) String reason) {
    }
}
