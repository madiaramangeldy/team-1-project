package kz.agromarket.common;

/**
 * Бизнес-қате.
 * code — машинаға арналған код, message — пайдаланушыға түсінікті мәтін,
 * status — кейін Spring Boot-та HTTP статус кодына айналады (400, 401, 403, 404, 409).
 */
public class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final int status;

    public DomainException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public DomainException(String code, String message) {
        this(code, message, 400);
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
