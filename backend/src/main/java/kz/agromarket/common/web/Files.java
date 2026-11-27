package kz.agromarket.common.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import kz.agromarket.common.storage.FileStorage;
import kz.agromarket.common.storage.FileUpload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/** Контроллерлерге ортақ көмекші: HTTP файлы ↔ біздің FileUpload / StoredFile */
public final class Files {

    private Files() {
    }

    /** MultipartFile (Spring) → FileUpload (сервис HTTP туралы білмеуі үшін) */
    public static FileUpload upload(MultipartFile file) {
        try {
            return new FileUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Файлды браузерге жіберу: inline — браузер ашып көрсетеді (фото, PDF) */
    public static ResponseEntity<InputStreamResource> inline(FileStorage.StoredFile file) {
        String type = file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(type))
                .contentLength(file.size())
                .body(new InputStreamResource(file.data()));
    }
}
