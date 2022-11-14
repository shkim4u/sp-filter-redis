package kr.co.starbucks.spfilterredis.filter.enums;

public enum ResponseCode {

    UNAUTHORIZED("0001", "SESSION 정보가 없습니다."),
    INTERNAL_SERVER_ERROR("0099", "서비스 접속이 원활하지 않습니다. 잠시 후 다시 이용해주세요."),
    UNKNOWN_ERROR("9999", "System error");

    private final String resultCode;

    private final String resultMessage;

    ResponseCode(String resultCode, String resultMessage) {
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
    }

    public String resultCode() { return this.resultCode; }

    public String resultMessage() { return this.resultMessage; }


}
