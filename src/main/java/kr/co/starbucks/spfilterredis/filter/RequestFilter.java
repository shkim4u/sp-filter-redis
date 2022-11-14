package kr.co.starbucks.spfilterredis.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.co.starbucks.spfilterredis.filter.enums.UserStatus;
import kr.co.starbucks.spfilterredis.filter.common.StaticValues;
import kr.co.starbucks.spfilterredis.filter.enums.RouteId;
import kr.co.starbucks.spfilterredis.filter.exception.InternalServerException;
import kr.co.starbucks.spfilterredis.filter.infrastructure.repositories.PrincipalMigrationStatusRedisReactiveRepository;
import kr.co.starbucks.spfilterredis.filter.infrastructure.repositories.PrincipalSessionRedisReactiveRepository;
import kr.co.starbucks.spfilterredis.filter.model.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Implementation Basics<br>
 * 이 필터의 구현 목적은 서비스 어플리케이션을 호출하는 특정 사용자의 SP 마이그레이션 여부를 판단하는 것이다.<br>
 * 즉, 특정 사용자의 세션 레디스와 마이그레이션 레디스를 살피고 이에 따라 마이그레이션 헤더 주입을 목적으로 한다.<br>
 * <p></p>
 * 필터의 동작 흐름은 다음과 같다.<br>
 * <li>Test</li>
 1. x-sp-customer-id 헤더
 1-1. 존재: 마이그레이션 상태 레디스 조회로 바로 이동
 1-2. 미존재:
 1-2-1. JSESSIONID 헤더
 1-2-2. JSESSIONID 쿠키
 1-2-3. 위 없을 시: 추가 동작없이 다운스트림으로 전달

 2. 마이그레이션 레디스에서 위 1에서 얻은 CustomerId로 XO_SP_ROUTE 상태 조회
 2-1. ACTIVE: x-sp-xo-migration-yn = Y 헤더 주입
 2-2. BLOCK: InternalServerException 발생
 2-3. 그외: x-sp-xo-migtation-yn = N 헤더 주입
 2-4. 미발견: 헤더 조작없이 다운스트리으로 요청 전달
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RequestFilter extends HttpFilter {

    private final PrincipalSessionRedisReactiveRepository principalSessionRedisReactiveRepository;
    private final PrincipalMigrationStatusRedisReactiveRepository principalMigrationStatusRedisReactiveRepository;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Mutable HTTP request from the source one.
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        getPrincipalSession(request)
            // 세션 레디스로부터 Principal을 찾았으면 마이그레이션 상태 조회
            .flatMap(p -> getPrincipalMigrationStatus(request, p, RouteId.XO_SP_ROUTE.name()))
            // "BLOCK" 상태이면 InternalServerException 발생
            .flatMap(p -> {
                if (Objects.equals(p.getUserStatus(), UserStatus.BLOCK.name())) {
                    return Mono.error(new InternalServerException("User status is `BLOCK`"));
                }
                return Mono.just(p);
            })
            /*
             * 조회된 마이그레이션 상태로부터 필요한 조작을 수행한다.
             * 현재 구현에서는 다운스트림 동작을 위해 "x-sp-xo-migration-yn" 헤더 주입.
             * - <특정 사용자, "XO_SP_ROUTE", ACTIVE>: "x-sp-xo-migration-yn" = "Y"
             * - 그외: "x-sp-xo-migration-yn" = "N"
             */
            .doOnNext(p -> putMigrationStatusHeader(mutableRequest, p))
            // 세션 및 마이그레이션 상태 레디스 조회가 끝나기를 기다린다. 먼 미래에 Reactive 전환이 완료되면 subscribe() 등을 활용하자.
            .block();

        // Hand over to the downstream.
        chain.doFilter(mutableRequest, response);
    }

    private void putMigrationStatusHeader(MutableHttpServletRequest request, Principal principal) {
        Assert.notNull(request, "HTTP 리퀘스트가 존재하지 않습니다");
        Assert.notNull(principal, "사용자 정보가 존재하지 않습니다");
        request.putHeader("x-sp-xo-migration-yn", Objects.equals(principal.getUserStatus(), UserStatus.ACTIVE.name()) ? "Y" : "N");
    }

    private Mono<Principal> getPrincipalSession(@NotNull HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("x-sp-customer-id"))
            .stream().findFirst()
            // "x-sp-customer-id" 헤더가 있으면 SckMbbrNo를 가진 Principal Mono 반환
            .map(id -> Mono.just(Principal.builder().sckMbbrNo(id).build()))
            // 없으면 헤더 혹은 쿠키에 존재하는 JSESSIONID로 세션 레디스 조회
            .orElse(
                // 헤더로부터
                Optional.ofNullable(request.getHeader(StaticValues.JSESSIONID))
                    .map(Mono::just)
                    // 헤더에 없으면 쿠키로부터
                    .orElse(
                        Optional.ofNullable(request.getCookies())
                            .flatMap(cookies -> Arrays.stream(cookies)
                                // JSSESIONID 쿠키가 있으면
                                .filter(c -> StaticValues.JSESSIONID.equals(c.getName()))
                                .findAny()
                            )
                            // 해당하는 쿠키가 있으면 쿠키 값 반환
                            .map(c -> Mono.just(c.getValue()))
                            .orElse(Mono.empty())
                    )
                    // 헤더나 쿠키에서 얻어온 JSESSIONID를 사용하여 세션 레디스 조회
                    .flatMap(sid ->
                        principalSessionRedisReactiveRepository.getPrincipalSession(sid)
                            .flatMap(p -> Mono.just(Principal.builder()
                                .userId(p.get(StaticValues.USER_ID))
                                .userName(p.get(StaticValues.USER_NAME))
                                .sckMbbrNo(p.get(StaticValues.SCK_MBBR_NO))
                                // TODO: 향후 UserType이 필요할 경우 principalSessionRedisReactiveRepository.getUserType() 호출
                                .build())
                            )
                            .switchIfEmpty(Mono.empty())
                    )
            );
    }

    private Mono<Principal> getPrincipalMigrationStatus(HttpServletRequest request, Principal principal, String routeId) {
        return principalMigrationStatusRedisReactiveRepository.getPrincipalMigrationStatus(principal.getSckMbbrNo(), routeId)
            .flatMap(s -> {
                principal.setUserStatus(s);
                return Mono.just(principal);
            })
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Baseline Assumption (기본 가정)
     * - 헤더에 "x-sp-customer-id"가 존재하더라도 마이그레이션 여부가 업스트림에서 검사되었음을 가정하지 않는다.
     * - 즉, "x-sp-customer-id" 헤더가 있으면 이를 사용하여 마이그레이션 여부를 검사하고,
     *   없을 경우 세션 레디스를 JSESSIONID 헤더 혹은 쿠키값으로 조회하여 Customer ID에 해당하는 SckMbbrNo 값을 찾는다.
     * - (업데이트) "x-sp-customer-id" 헤더가 존재하더라도 5대 헤더는 획득이 필요 -> 레디스 세션 탐색
     * @param request
     * @return
     */
//    private String findCustomerId(final HttpServletRequest request) {
//        /*
//         * https://confl.sinc.co.kr/pages/viewpage.action?pageId=135397561
//         * "x-sp-customer-id" search route ID:
//         * 메모리 레디스의 마이그레이션 대상 조회에 바로 사용 가능
//         * - 1: (External) APP_SR_ROUTE
//         * - 2: (External) APP_SP_ROUTE
//         * - 4: (Internal) WEB_SR_ROUTE
//         */
//        String headerCustomerId = request.getHeader("x-sp-customer-id");
//        String sessionCustomerId = findCustomerIdFromSession(request);
//    }

//    private String findCustomerIdFromSession(final HttpServletRequest request) {
//        /*
//         * https://confl.sinc.co.kr/pages/viewpage.action?pageId=135397561
//         * "JSESSIONID" search route ID:
//         * 세션 레디스로부터 "sckMbbrNo"를 조회하고 이를 통해 메모리 레디스의 마이그레이션 대상 조회
//         * - 3: (External) SP_ROUTE
//         * - 5: (Internal) WEB_SP_ROUTE
//         * - 6: (Internal) WEB_SP_ROUTE_NEW
//         * - 7: (공통) SR_ROUTE (Only to SR)
//         */
//        // request.getHeader() returns the first one.
//        String jsessionid = Optional.ofNullable(request.getHeader("JSESSIONID"))
//            // 헤더에 없으면 쿠키로부터.
//            .orElse(Optional.ofNullable(request.getCookies())
//                    .flatMap(cookies -> Arrays.stream(cookies)
//                        // JSESSIONID 쿠키가 있으면
//                        .filter(c -> "JSESSIONID".equals(c.getName()))
//                        .findAny()
//                    )
//                    // 해당하는 쿠키가 있으면 값 반환
//                    .map(c -> c.getValue())
//                    // 이외에는 null 반환
//                    .orElse(null)
//            );
//
//    }

}
