package kz.agromarket.catalog.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<RegionEntity, Long> {

    Optional<RegionEntity> findByName(String name);
}
