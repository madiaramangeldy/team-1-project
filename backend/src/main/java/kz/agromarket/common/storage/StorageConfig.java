package kz.agromarket.common.storage;

import io.minio.MinioClient;
import kz.agromarket.config.AgroMarketProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Қай сақтау орнын қолданатынымызды баптаулар шешеді — сервистер бұл туралы білмейді */
@Configuration
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(AgroMarketProperties properties) {
        AgroMarketProperties.Storage storage = properties.storage();
        if (storage == null || storage.endpoint() == null || storage.endpoint().isBlank()) {
            return new InMemoryFileStorage();
        }
        MinioClient client = MinioClient.builder()
                .endpoint(storage.endpoint())
                .credentials(storage.accessKey(), storage.secretKey())
                .build();
        return new MinioFileStorage(client, storage.bucket());
    }
}
