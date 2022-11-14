package kr.co.starbucks.spfilterredis.filter.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class Principal {
    private String userId;
    private String userName;
    private String sckMbbrNo;
    private String userType;
    private String appId;
    private String userStatus;
}
