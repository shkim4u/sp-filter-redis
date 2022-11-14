package kr.co.starbucks.spfilterredis.filter.exception;

import kr.co.starbucks.spfilterredis.filter.enums.ResponseCode;
import org.springframework.http.HttpStatus;

public class InternalServerException extends GlobalException {

    private static final ResponseCode responseCode = ResponseCode.INTERNAL_SERVER_ERROR;

    public InternalServerException(HttpStatus status) {
        super(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }

    public InternalServerException(HttpStatus status, String reason, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
    }

    @Override
    public String getResultCode() {
        return responseCode.resultCode();
    }

    @Override
    public String getResultMessage() {
        return responseCode.resultMessage();
    }
}
