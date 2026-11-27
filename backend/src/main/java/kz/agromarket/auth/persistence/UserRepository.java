package kz.agromarket.auth.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository — дерекқормен жұмыс істейтін интерфейс.
 * Spring Data әдіс атынан SQL-ды өзі жасайды:
 *   findByEmailIgnoreCase  →  SELECT * FROM users WHERE lower(email) = lower(?)
 * Ал save(), findById(), findAll(), delete() JpaRepository-де дайын тұр.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
