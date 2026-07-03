package com.bank.shared.exception;

/**
 * Typed business error carrying an application error <b>code</b>, a human
 * message, and the HTTP <b>status</b> to surface. Thrown by domain/services and
 * translated to the {@link ApiError} envelope by {@link GlobalExceptionHandler}.
 *
 * <p>Using a single exception type with an explicit status keeps the money path
 * readable — a rule violation is {@code throw new ApiException("INSUFFICIENT_FUNDS", ..., 422)}
 * rather than a sprawl of bespoke exception classes.
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;

    public ApiException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
