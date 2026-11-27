package kz.agromarket.common.storage;

import java.io.InputStream;
import java.util.List;
import kz.agromarket.common.UploadedFile;

/**
 * Контроллерден сервиске берілетін файл (MultipartFile-дан тәуелсіз — сервис HTTP туралы білмейді).
 * Тексеру ережелері домендік UploadedFile-да: кеңейтім және 5 МБ шегі.
 */
public record FileUpload(String fileName, String contentType, long size, InputStream data) {

    public static final List<String> DOCUMENT_EXTENSIONS = List.of("pdf", "jpg", "jpeg");
    public static final List<String> PHOTO_EXTENSIONS = List.of("jpg", "jpeg", "png");

    public String extension() {
        return asUploadedFile().extension();
    }

    public void validate(List<String> allowedExtensions, String formatMessage) {
        asUploadedFile().validate(allowedExtensions, formatMessage);
    }

    private UploadedFile asUploadedFile() {
        return new UploadedFile(fileName == null ? "" : fileName, size);
    }
}
