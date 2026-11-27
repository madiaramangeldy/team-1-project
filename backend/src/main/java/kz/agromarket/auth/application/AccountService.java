package kz.agromarket.auth.application;

import java.util.List;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.VerificationStatus;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.auth.persistence.FarmerProfileRepository;
import kz.agromarket.auth.persistence.UserEntity;
import kz.agromarket.auth.persistence.UserRepository;
import kz.agromarket.auth.web.AuthDtos.FarmerResponse;
import kz.agromarket.auth.web.AuthDtos.LoginRequest;
import kz.agromarket.auth.web.AuthDtos.LoginResponse;
import kz.agromarket.auth.web.AuthDtos.ProfileRequest;
import kz.agromarket.auth.web.AuthDtos.SignUpRequest;
import kz.agromarket.auth.web.AuthDtos.UserResponse;
import kz.agromarket.catalog.persistence.RegionEntity;
import kz.agromarket.catalog.persistence.RegionRepository;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.EventBus;
import kz.agromarket.common.storage.FileStorage;
import kz.agromarket.common.storage.FileUpload;
import kz.agromarket.security.AuthUser;
import kz.agromarket.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-01 Тіркелу және кіру, US-02 Фермерді верификациялау — дерекқормен (JPA) жұмыс істейтін нұсқа.
 *
 * Бизнес-ережелер домендік AuthService-пен бірдей, бірақ деректер PostgreSQL-да сақталады,
 * құпиясөз BCrypt-пен хэштеледі, ал кіргенде JWT токен беріледі.
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final FarmerProfileRepository farmers;
    private final RegionRepository regions;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorage storage;
    private final EventBus eventBus;

    public AccountService(UserRepository users, FarmerProfileRepository farmers, RegionRepository regions,
                          PasswordEncoder passwordEncoder, JwtService jwtService,
                          FileStorage storage, EventBus eventBus) {
        this.users = users;
        this.farmers = farmers;
        this.regions = regions;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.storage = storage;
        this.eventBus = eventBus;
    }

    // ---------- US-01 ----------

    public UserResponse register(SignUpRequest request) {
        if (request.role() == Role.MODERATOR) {
            throw new DomainException("FORBIDDEN", "Модератор ретінде өзіңіз тіркеле алмайсыз", 403);
        }
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new DomainException("EMAIL_TAKEN", "Бұл email бұрын тіркелген", 409);
        }

        // Фермердің өңірін алдымен тексереміз: қате болса, пайдаланушы мүлде сақталмайды
        RegionEntity region = request.role() == Role.FARMER ? farmerRegion(request) : null;

        UserEntity user = users.save(new UserEntity(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.role()));

        FarmerProfileEntity profile = null;
        if (region != null) {
            profile = farmers.save(new FarmerProfileEntity(user, region, farmName(request)));
        }
        return UserResponse.of(user, profile);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = users.findByEmailIgnoreCase(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", "Email немесе құпиясөз қате", 401));

        String token = jwtService.issue(new AuthUser(user.getId(), user.getEmail(), user.getRole()));
        FarmerProfileEntity profile = farmers.findByUserId(user.getId()).orElse(null);
        return new LoginResponse(token, "Bearer", jwtService.ttlSeconds(), UserResponse.of(user, profile));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Пайдаланушы табылмады", 404));
        return UserResponse.of(user, farmers.findByUserId(userId).orElse(null));
    }

    public UserResponse updateProfile(Long userId, ProfileRequest request) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Пайдаланушы табылмады", 404));
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        user.updateProfile(request.name().trim(), phone);

        FarmerProfileEntity profile = farmers.findByUserId(userId).orElse(null);
        if (profile != null && request.farmName() != null && !request.farmName().isBlank()) {
            profile.rename(request.farmName().trim());
        }
        return UserResponse.of(user, profile);
    }

    // ---------- US-02 ----------

    @Transactional(readOnly = true)
    public List<FarmerResponse> pendingFarmers() {
        return farmers.findByVerificationStatus(VerificationStatus.PENDING).stream()
                .map(FarmerResponse::of)
                .toList();
    }

    /** US-01: фермер құжатын жүктейді (PDF/JPG, 5 МБ-қа дейін) → мәртебе PENDING, модератор тексереді */
    public FarmerResponse uploadDocument(Long userId, FileUpload file) {
        FarmerProfileEntity profile = farmers.findByUserId(userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Фермер профилі табылмады", 404));
        file.validate(FileUpload.DOCUMENT_EXTENSIONS, "Құжат PDF немесе JPG форматында болуы керек");
        String key = storage.store("documents", file.extension(), file.contentType(), file.data(), file.size());
        profile.uploadDocument(key);
        return FarmerResponse.of(profile);
    }

    /** Модератор фермердің құжатын ашып көреді */
    @Transactional(readOnly = true)
    public FileStorage.StoredFile document(Long farmerProfileId) {
        String key = profile(farmerProfileId).getDocumentUrl();
        if (key == null) {
            throw new DomainException("NOT_FOUND", "Фермер әлі құжат жүктемеген", 404);
        }
        return storage.load(key);
    }

    public FarmerResponse approve(Long farmerProfileId) {
        FarmerProfileEntity profile = profile(farmerProfileId);
        if (profile.getDocumentUrl() == null) {
            throw new DomainException("NO_DOCUMENT", "Құжатсыз фермерді растауға болмайды", 409);
        }
        profile.approve(); // @Transactional: өзгеріс транзакция соңында өзі сақталады
        eventBus.publish(new FarmerVerifiedEvent(profile)); // Observer: фермерге хабарлама
        return FarmerResponse.of(profile);
    }

    public FarmerResponse reject(Long farmerProfileId, String reason) {
        FarmerProfileEntity profile = profile(farmerProfileId);
        profile.reject(reason);
        eventBus.publish(new FarmerVerifiedEvent(profile));
        return FarmerResponse.of(profile);
    }

    // ---------- Көмекші әдістер ----------

    private FarmerProfileEntity profile(Long id) {
        return farmers.findById(id)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Фермер табылмады", 404));
    }

    private RegionEntity farmerRegion(SignUpRequest request) {
        if (request.regionId() == null) {
            throw new DomainException("VALIDATION", "Фермер үшін өңір (regionId) міндетті");
        }
        return regions.findById(request.regionId())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Өңір табылмады", 404));
    }

    private static String farmName(SignUpRequest request) {
        return request.farmName() == null || request.farmName().isBlank() ? request.name() : request.farmName();
    }
}
