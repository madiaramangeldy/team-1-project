package kz.agromarket.common.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kz.agromarket.common.DomainException;

/** Жадтағы сақтау орны: unit-тесттер үшін және MinIO бапталмаған кезде (agromarket.storage.endpoint бос) */
public class InMemoryFileStorage implements FileStorage {

    private record Entry(byte[] bytes, String contentType) {
    }

    private final Map<String, Entry> files = new ConcurrentHashMap<>();

    @Override
    public String store(String folder, String extension, String contentType, InputStream data, long size) {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        try {
            files.put(key, new Entry(data.readAllBytes(), contentType));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return key;
    }

    @Override
    public StoredFile load(String key) {
        Entry entry = files.get(key);
        if (entry == null) {
            throw new DomainException("NOT_FOUND", "Файл табылмады", 404);
        }
        return new StoredFile(new ByteArrayInputStream(entry.bytes()), entry.contentType(), entry.bytes().length);
    }
}
