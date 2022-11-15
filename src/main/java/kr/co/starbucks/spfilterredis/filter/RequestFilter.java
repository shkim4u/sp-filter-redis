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
import kr.co.starbucks.spfilterredis.filter.common.StaticValues;
import kr.co.starbucks.spfilterredis.filter.enums.RouteId;
import kr.co.starbucks.spfilterredis.filter.enums.UserStatus;
import kr.co.starbucks.spfilterredis.filter.exception.InternalServerException;
import kr.co.starbucks.spfilterredis.filter.infrastructure.repositories.PrincipalMigrationStatusRedisReactiveRepository;
import kr.co.starbucks.spfilterredis.filter.infrastructure.repositories.PrincipalSessionRedisReactiveRepository;
import kr.co.starbucks.spfilterredis.filter.model.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * <h2><b><i><u>Implementation Basics</u></i></b></h2>
 * 이 필터의 구현 목적은 서비스 어플리케이션을 호출하는 특정 사용자의 SP 마이그레이션 여부를 판단하는 것이다.<br>
 * 즉, 특정 사용자의 세션 레디스와 마이그레이션 레디스를 살피고 이에 따라 마이그레이션 헤더 주입을 목적으로 한다.<br>
 * <p></p>
 * 필터의 동작 흐름은 다음과 같다. (참고 1: <a href="https://miro.com/app/board/uXjVPIXkmfM=/?moveToWidget=3458764538603035763&cot=14">마이그레이션 - API 분기처리</a>,
 * 참고 2: <a href="https://confl.sinc.co.kr/pages/viewpage.action?pageId=135397561">리버스 프록시 분기 기준</a>) <br>
 * <p></p>
 * 1. x-sp-customer-id 헤더 검사<br>
 * 1.1. 존재하면 마이그레이션 상태 레디스 조회로 바로 이동<br>
 * 1.2. 존재하지 않으면 헤더 혹은 쿠키의 JSESSIONID를 키값으로 세션 레디스에서 SckMbbrNo 값을 찾음<br>
 * 1.3. 위 1.1., 1.2.에서 CustomerId에 해당하는 값(x-sp-customer-id 혹은 SckMbbrNo)을 찾지 못하면 다운스트림으로 트래픽을 바이패스 전달<br>
 * <p></p>
 * 2. 마이그레이션 레디스에서 위 1에서 얻은 CustomerId에 해당하는 값으로 XO_SP_ROUTE 상태 조회<br>
 * 2-1. BLOCK: InternalServerException 발생<br>
 * 2.2. ACTIVE: ("x-sp-xo-migration-yn": Y) 헤더를 주입하여 다운스트림으로 트래픽을 전달<br>
 * 2-3. 그외 값: ("x-sp-xo-migtation-yn": "N") 헤더를 주입하여 다운스트림으로 트래픽을 전달<br>
 * 2-4. 미발견: 헤더 조작없이 다운스트림으로 트래픽을 바이패스 전달<br>
 *
 * <p></p>
 * <h2><b><i><u>참고</u></i></b></h2>
 * 이 필터 및 관련 Configuration과 Bean들은 아래 속성(Property) 값들이 프로파일 상에서 발견될 경우에만 주입되어 동작한다.<br>
 * * ${spring.redis.principal-session.host} <br>
 * * ${spring.redis.principal-session.port} <br>
 * * ${spring.redis.principal-migration-status.host} <br>
 * * ${spring.redis.principal-migration-status.port} <br>
 *
 * @author Sanghyoun Kim
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ConditionalOnExpression(
    "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-session.host:}') and "
        + "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-session.port:}') and "
        + "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-migration-status.host:}') and "
        + "!T(org.springframework.util.StringUtils).isEmpty('${spring.redis.principal-migration-status.port:}')"
)
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
                    )
            );
    }

    private Mono<Principal> getPrincipalMigrationStatus(HttpServletRequest request, Principal principal, String routeId) {
        return principalMigrationStatusRedisReactiveRepository.getPrincipalMigrationStatus(principal.getSckMbbrNo(), routeId)
            .flatMap(s -> {
                principal.setUserStatus(s);
                return Mono.just(principal);
            });
    }
}
