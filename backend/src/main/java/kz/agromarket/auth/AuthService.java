package kz.agromarket.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.EventBus;
import kz.agromarket.common.Events;
import kz.agromarket.common.Ids;
import kz.agromarket.common.UploadedFile;

/**
 * US-01 Фермердің тіркелуі, US-02 Фермерді верификациялау.
 * Жауапкершілігі: тек пайдаланушылар, рөлдер және верификация (SOLID — S).
 */
public class AuthService {

    private static final List<String> DOCUMENT_EXTENSIONS = List.of("pdf", "jpg", "jpeg");

    private final Map<String, User> users = new LinkedHashMap<>();
    private final EventBus eventBus;

    public AuthService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    // ---------- US-01 ----------

    public User register(RegisterRequest request) {
        if (request.email() == null || !request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new DomainException("VALIDATION", "Email дұрыс емес");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new DomainException("VALIDATION", "Құпиясөз кемінде 8 таңбадан тұруы керек");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new DomainException("VALIDATION", "Аты міндетті");
        }
        if (findByEmail(request.email()).isPresent()) {
            throw new DomainException("EMAIL_TAKEN", "Бұл email бұрын тіркелген", 409);
        }

        User user = new User(
                Ids.next("user"),
                request.email().toLowerCase(),
                request.name(),
                request.role(),
                PasswordHasher.hash(request.password()));
        users.put(user.getId(), user);
        return user;
    }

    public User login(String email, String password) {
        return findByEmail(email)
                .filter(u -> PasswordHasher.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", "Email немесе құпиясөз қате", 401));
    }

    public User uploadDocument(String farmerId, UploadedFile file) {
        User farmer = getUser(farmerId);
        if (farmer.getRole() != Role.FARMER) {
            throw new DomainException("FORBIDDEN", "Құжатты тек фермер жүктей алады", 403);
        }
        file.validate(DOCUMENT_EXTENSIONS, "Құжат PDF немесе JPG форматында болуы керек");
        farmer.setDocumentUrl("minio://documents/" + farmerId + "/" + file.fileName());
        farmer.setVerificationStatus(VerificationStatus.PENDING);
        return farmer;
    }

    // ---------- US-02 ----------

    public List<User> listPendingFarmers(String moderatorId) {
        assertRole(moderatorId, Role.MODERATOR);
        return users.values().stream()
                .filter(u -> u.getRole() == Role.FARMER && u.getVerificationStatus() == VerificationStatus.PENDING)
                .toList();
    }

    public User approveFarmer(String moderatorId, String farmerId) {
        return decide(moderatorId, farmerId, VerificationStatus.APPROVED, null);
    }

    public User rejectFarmer(String moderatorId, String farmerId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("VALIDATION", "Бас тарту себебін көрсету міндетті");
        }
        return decide(moderatorId, farmerId, VerificationStatus.REJECTED, reason);
    }

    private User decide(String moderatorId, String farmerId, VerificationStatus status, String reason) {
        assertRole(moderatorId, Role.MODERATOR);
        User farmer = getUser(farmerId);
        if (farmer.getRole() != Role.FARMER) {
            throw new DomainException("VALIDATION", "Бұл пайдаланушы фермер емес");
        }
        farmer.setVerificationStatus(status);
        farmer.setRejectReason(reason);

        // Observer: фермерге хабарламаны Notifications модулі жібереді
        eventBus.publish(new Events.FarmerVerified(farmerId, status == VerificationStatus.APPROVED, reason));
        return farmer;
    }

    // ---------- Көмекші әдістер ----------

    public User getUser(String id) {
        User user = users.get(id);
        if (user == null) {
            throw new DomainException("NOT_FOUND", "Пайдаланушы табылмады", 404);
        }
        return user;
    }

    public boolean isVerifiedFarmer(String id) {
        User user = users.get(id);
        return user != null && user.getRole() == Role.FARMER
                && user.getVerificationStatus() == VerificationStatus.APPROVED;
    }

    public void assertRole(String userId, Role role) {
        if (getUser(userId).getRole() != role) {
            throw new DomainException("FORBIDDEN", "Бұл әрекетке рұқсат жоқ", 403);
        }
    }

    private Optional<User> findByEmail(String email) {
        return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }
}
