package kz.agromarket.catalog;

/**
 * Фото сақтау интерфейсі. CatalogService тек осы интерфейсті біледі.
 * Қазір InMemoryPhotoStorage, кейін MinIO нұсқасымен ауыстырылады — CatalogService өзгермейді (SOLID — D).
 */
public interface PhotoStorage {

    String upload(String folder, String fileName);

    class InMemory implements PhotoStorage {
        @Override
        public String upload(String folder, String fileName) {
            return "minio://products/" + folder + "/" + fileName;
        }
    }
}
