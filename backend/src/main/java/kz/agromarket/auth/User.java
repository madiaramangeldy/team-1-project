package kz.agromarket.auth;

public class User {

    private final String id;
    private final String email;
    private final String name;
    private final Role role;
    private final String passwordHash;

    /** Тек FARMER үшін */
    private VerificationStatus verificationStatus;
    private String documentUrl;
    private String rejectReason;

    public User(String id, String email, String name, Role role, String passwordHash) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.passwordHash = passwordHash;
        this.verificationStatus = role == Role.FARMER ? VerificationStatus.PENDING : null;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getDocumentUrl() { return documentUrl; }
    public String getRejectReason() { return rejectReason; }

    void setVerificationStatus(VerificationStatus status) { this.verificationStatus = status; }
    void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
    void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
