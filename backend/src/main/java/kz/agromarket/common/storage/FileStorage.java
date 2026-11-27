package kz.agromarket.common.storage;

import java.io.InputStream;

/**
 * Файл сақтау орны — сервистер тек осы интерфейсті біледі.
 *
 * ADAPTER паттерні: MinioFileStorage MinIO SDK-ның өз API-ын (PutObjectArgs, GetObjectArgs...)
 * біздің қарапайым интерфейсімізге бейімдейді. Ертең Amazon S3 немесе диск керек болса —
 * жаңа адаптер жазамыз, сервистер өзгермейді.
 */
public interface FileStorage {

    /**
     * Файлды сақтап, оның кілтін қайтарады (мысалы, products/3f2a…c1.jpg).
     * Кілт дерекқорға жазылады, файлдың өзі MinIO-да тұрады.
     */
    String store(String folder, String extension, String contentType, InputStream data, long size);

    StoredFile load(String key);

    record StoredFile(InputStream data, String contentType, long size) {
    }
}
