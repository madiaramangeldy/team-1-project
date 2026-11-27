package kz.agromarket.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import kz.agromarket.common.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Барлық контроллерлердің қателерін бір жерде ұстап, бірыңғай JSON форматына айналдырады.
 * Сондықтан контроллерлерде try/catch жазудың қажеті жоқ.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Бизнес-қате: DomainException-ның status өрісі HTTP статусқа айналады (400, 403, 404, 409...) */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException e, HttpServletRequest request) {
        ApiError body = ApiError.of(e.getCode(), e.getMessage(), e.getStatus(), request.getRequestURI());
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    /** DTO валидациясы (@NotBlank, @Positive ...) өтпесе — 400 және әр өрістің қатесі */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ApiError body = new ApiError("VALIDATION", "Деректер дұрыс толтырылмаған", 400,
                request.getRequestURI(), Instant.now(), fields);
        return ResponseEntity.badRequest().body(body);
    }

    /** JSON бұзық немесе мән қате түрде (мысалы, role: "ADMIN") — 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        ApiError body = ApiError.of("BAD_REQUEST", "Сұрау денесі дұрыс емес (JSON немесе мән форматы)", 400,
                request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /** URL параметрі қате түрде (мысалы, page=abc) — 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ApiError body = ApiError.of("BAD_REQUEST", "Параметр дұрыс емес: " + e.getName(), 400, request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    /** Жоқ мекенжай (мысалы, браузер сұрайтын /favicon.ico) — 404, логқа қате ретінде жазылмайды */
    /** application.yml-дағы 5 МБ шегінен асқан файл */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        ApiError body = ApiError.of("VALIDATION", "Файл көлемі 5 МБ-тан аспауы керек", 400, request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleNoFile(MissingServletRequestPartException e, HttpServletRequest request) {
        ApiError body = ApiError.of("VALIDATION", "Файл таңдалмаған (file өрісі)", 400, request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException e, HttpServletRequest request) {
        ApiError body = ApiError.of("NOT_FOUND", "Мұндай мекенжай жоқ", 404, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** Күтпеген қате — пайдаланушыға ішкі мәліметтерді көрсетпейміз, тек логқа жазамыз */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), e);
        ApiError body = ApiError.of("INTERNAL_ERROR", "Ішкі қате орын алды", 500, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
