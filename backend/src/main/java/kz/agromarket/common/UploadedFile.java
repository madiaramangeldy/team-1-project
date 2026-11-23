package kz.agromarket.common;

import java.util.List;

/** Жүктелген файл (Spring Boot-та MultipartFile болады). */
public record UploadedFile(String fileName, long sizeBytes) {

    public static final long MAX_SIZE = 5L * 1024 * 1024; // 5 МБ

    public String extension() {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    public void validate(List<String> allowedExtensions, String formatMessage) {
        if (!allowedExtensions.contains(extension())) {
            throw new DomainException("VALIDATION", formatMessage);
        }
        if (sizeBytes > MAX_SIZE) {
            throw new DomainException("VALIDATION", "Файл көлемі 5 МБ-тан аспауы керек");
        }
    }
}
