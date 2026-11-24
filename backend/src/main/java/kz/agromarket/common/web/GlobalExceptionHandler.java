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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    /** Күтпеген қате — пайдаланушыға ішкі мәліметтерді көрсетпейміз, тек логқа жазамыз */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Күтпеген қате: {}", request.getRequestURI(), e);
        ApiError body = ApiError.of("INTERNAL_ERROR", "Ішкі қате орын алды", 500, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
