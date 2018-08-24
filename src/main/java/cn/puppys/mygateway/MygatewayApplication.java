package cn.puppys.mygateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.function.Function;


@SpringBootApplication
public class MygatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MygatewayApplication.class, args);
    }

    @Bean
    public KeyResolver myKeyResolver(){
        return new MyKeyResolver();
    }

    @Bean
    public RateLimiter rateLimiter(){
        return new MyRateLimiter();
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,RateLimiter rateLimiter,KeyResolver myKeyResolver) {
        Function<Principal, String> getName = Principal::getName;
        return builder.routes()
                .route("first", r ->
                                r.header("X-Request-Id", "\\d+")
//                                .and().cookie("abc","123")
                                        .filters(f -> f.addRequestHeader("addRequestHeader", "addRequestHeader1")
                                                .addRequestParameter("addRequestParameter", "addRequestParameter1")//下游服务器通过request.getParameter()获取
                                                .addResponseHeader("addResponseHeader", "addResponseHeader1")
                                                .preserveHostHeader()
                                                .requestRateLimiter(config -> {
                                                    config.setKeyResolver(myKeyResolver);

                                                    config.setRateLimiter(rateLimiter);
                                                })
                                        )
                                        .uri("lb://application-commission-api/commission/api/calculateCommissionStrategy")
                                        .order(1)//order数值越低优先级越高
                )
//                .route("second",r ->
//                        r.header("X-Request-Id","\\d+")
//                                .uri("http://localhost:7798/commission/api/calculateCommissionStrategy")
//                        .order(2))
                .build();
    }
}
