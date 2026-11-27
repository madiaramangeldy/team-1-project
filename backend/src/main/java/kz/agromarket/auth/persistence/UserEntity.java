package kz.agromarket.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kz.agromarket.auth.Role;

/**
 * `users` кестесі (V1__init.sql).
 *
 * Entity — дерекқордағы бір кестенің Java-дағы бейнесі: әр өріс — бір баған.
 * Hibernate бұл класты кестемен өзі байланыстырады, SQL жазудың қажеті жоқ.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // BIGSERIAL: ID-ді PostgreSQL өзі береді
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING) // дерекқорда 'FARMER' түрінде сақталады, 0/1/2 емес
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserEntity() {
        // JPA үшін бос конструктор міндетті
    }

    public UserEntity(String email, String passwordHash, String name, String phone, Role role) {
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.role = role;
    }

    /** Профильді өзгерту: email мен рөл өзгермейді */
    public void updateProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
