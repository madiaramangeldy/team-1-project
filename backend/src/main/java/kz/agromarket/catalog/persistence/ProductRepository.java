package kz.agromarket.catalog.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Өнімдер репозиторийі.
 * JpaSpecificationExecutor — сүзгілерді динамикалық құрастыру үшін (ProductSpecifications):
 * тек толтырылған сүзгілер ғана SQL-ға қосылады.
 */
public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    /** Фермер кабинеті: өз өнімдері (верификация мәртебесіне қарамастан) */
    List<ProductEntity> findByFarmerUserIdOrderByIdDesc(Long userId);
}
