package kz.agromarket.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.agromarket.common.storage.FileStorage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Өнім фотоларын көрсету (токенсіз, каталог сияқты ашық).
 * Фермер құжаттары мұнда емес — оларды тек модератор ашады (/api/moderation/farmers/{id}/document).
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "Өнім фотолары (MinIO-дан)")
public class FileController {

    private final FileStorage storage;

    public FileController(FileStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/products/{name}")
    @Operation(summary = "Өнім фотосы")
    public ResponseEntity<InputStreamResource> productPhoto(@PathVariable String name) {
        return Files.inline(storage.load("products/" + name));
    }
}
