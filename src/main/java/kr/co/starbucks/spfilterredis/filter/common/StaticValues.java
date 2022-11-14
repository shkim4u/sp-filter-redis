package kr.co.starbucks.spfilterredis.filter.common;

public class StaticValues {
    private StaticValues(){}

    public static final String JSESSIONID = "JSESSIONID";

    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String USER_TYPE = "userType";
    public static final String SCK_MBBR_NO = "sckMbbrNo";
    public static final String APP_ID = "appId";

    /**
     * header
     */
    public static final String X_SP_USER_ID = "x-sp-user-id";
    public static final String X_SP_CUSTOMER_ID = "x-sp-customer-id";
    public static final String X_SP_USER_NAME = "x-sp-user-name";
    public static final String X_SP_USER_TYPE = "x-sp-user-type"; //사용자유형(1:MSR회원 2:웹회원 3:비회원 4:준회원)
    public static final String X_SP_APP_ID = "x-sp-app-id";

    public static final String RESULT_CODE = "resultCode";

    public static final String RESULT_MESSAGE = "resultMessage";

    /**
     * Channel class
     *
     */
    public static final String HTTPS_PROTOCOL = "https";
}
