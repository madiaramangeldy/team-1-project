package kz.agromarket.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import kz.agromarket.auth.VerificationStatus;
import kz.agromarket.catalog.persistence.RegionEntity;
import kz.agromarket.common.DomainException;

/**
 * `farmer_profiles` кестесі. Байланыстар:
 *  - users 1 : 1 farmer_profiles (әр фермерде бір профиль)
 *  - regions 1 : N farmer_profiles
 */
@Entity
@Table(name = "farmer_profiles")
public class FarmerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private RegionEntity region;

    @Column(name = "farm_name", nullable = false, length = 150)
    private String farmName;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    protected FarmerProfileEntity() {
        // JPA үшін бос конструктор міндетті
    }

    public FarmerProfileEntity(UserEntity user, RegionEntity region, String farmName) {
        this.user = user;
        this.region = region;
        this.farmName = farmName;
    }

    // ---------- US-02: бизнес-әрекеттер entity ішінде ----------

    public void rename(String farmName) {
        this.farmName = farmName;
    }

    public void uploadDocument(String url) {
        this.documentUrl = url;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public void approve() {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("VALIDATION", "Бас тарту себебін көрсету міндетті");
        }
        this.verificationStatus = VerificationStatus.REJECTED;
        this.rejectReason = reason;
    }

    /** US-13: пікірлердің орташа бағасы */
    public void updateRating(BigDecimal rating) {
        this.rating = rating;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.APPROVED;
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public RegionEntity getRegion() { return region; }
    public String getFarmName() { return farmName; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getDocumentUrl() { return documentUrl; }
    public String getRejectReason() { return rejectReason; }
    public BigDecimal getRating() { return rating; }
}
