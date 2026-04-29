package io.gemboot;

enum StatusCodes {

    // 1x - Input expected
    INPUT(10, "Input"),
    SENSITIVE_INPUT(11, "Sensitive input"),

    // 2x - Success
    SUCCESS(20, "Success"),

    // 3x - Redirection
    TEMPORARY_REDIRECT(30, "Temporary redirection"),
    PERMANENT_REDIRECT(31, "Permanent redirection"),

    // 4x - Temporary failure
    TEMPORARY_FAILURE(40, "Temporary failure"),
    SERVER_UNAVAILABLE(41, "Server unavailable"),
    CGI_ERROR(42, "CGI error"),
    PROXY_ERROR(43, "Proxy error"),
    SLOW_DOWN(44, "Slow down"),

    // 5x - Permanent failure
    PERMANENT_FAILURE(50, "Permanent failure"),
    NOT_FOUND(51, "Not found"),
    GONE(52, "Gone"),
    PROXY_REQUEST_REFUSED(53, "Proxy request refused"),
    BAD_REQUEST(59, "Bad request"),

    // 6x - Client certificates
    CLIENT_CERTIFICATE_REQUIRED(60, "Client certificate required"),
    CERTIFICATE_NOT_AUTHORIZED(61, "Certificate not authorized"),
    CERTIFICATE_NOT_VALID(62, "Certificate not valid");

    private final int code;
    private final String reason;

    StatusCodes(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int code() {
        return code;
    }

    public String reason() {
        return reason;
    }
}
