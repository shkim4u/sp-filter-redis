package kr.co.starbucks.spfilterredis.filter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

abstract class GlobalException extends ResponseStatusException {

    private static final long serialVersionUID = -1L;

    protected GlobalException(HttpStatus status) {
        super(status);
    }

    protected GlobalException(HttpStatus status, String reason) {
        super(status, reason);
    }

    protected GlobalException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    abstract String getResultCode();

    abstract String getResultMessage();
}
