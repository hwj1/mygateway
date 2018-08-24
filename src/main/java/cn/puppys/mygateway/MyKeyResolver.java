package cn.puppys.mygateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

public class MyKeyResolver implements KeyResolver {
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {


        HttpHeaders headers = exchange.getRequest().getHeaders();
        List<String> strings = headers.get("X-Forwarded-For");
        String a = null;
        if (strings != null){
            a = strings.get(0);
        }
        System.out.println(a);
        Mono<String> just = Mono.just(a).switchIfEmpty(Mono.just(""));
        return just;
    }

    public static void main(String[] args) {
        Mono<String> aaa = Mono.just("AAA");
        Mono<char[]> mono = aaa.flatMap(s -> {
            System.out.println(s);
            return Mono.just(s.toCharArray());
        });

        Mono<String> map = mono.map(chars -> {
            return chars.toString();
        });

    }
}
