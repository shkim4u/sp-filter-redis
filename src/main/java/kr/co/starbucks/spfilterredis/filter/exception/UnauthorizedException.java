package kr.co.starbucks.spfilterredis.filter.exception;

import kr.co.starbucks.spfilterredis.filter.enums.ResponseCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends GlobalException {

    private static final long serialVersionUID = -1L;
    private static final ResponseCode responseCode = ResponseCode.UNAUTHORIZED;

    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String reason) {
        super(HttpStatus.UNAUTHORIZED, reason);
    }

    public UnauthorizedException(String reason, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, reason, cause);
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
