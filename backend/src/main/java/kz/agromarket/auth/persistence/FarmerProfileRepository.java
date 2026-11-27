package kz.agromarket.auth.persistence;

import java.util.List;
import java.util.Optional;
import kz.agromarket.auth.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerProfileRepository extends JpaRepository<FarmerProfileEntity, Long> {

    Optional<FarmerProfileEntity> findByUserId(Long userId);

    /** US-02: модераторға тексеруді күтіп тұрған фермерлер */
    List<FarmerProfileEntity> findByVerificationStatus(VerificationStatus status);
}
