package cn.puppys.mygateway;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.ApplicationEvent;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MyRateLimiter extends AbstractRateLimiter<MyRateLimiter.Config> {
    private Log log = LogFactory.getLog(getClass());

    public static final String CONFIGURATION_PROPERTY_NAME = "redis-rate-limiter";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";
    public static final String BURST_CAPACITY_HEADER = "X-RateLimit-Burst-Capacity";

    /** The name of the header that returns number of remaining requests during the current second. */
    private String remainingHeader = REMAINING_HEADER;

    /** The name of the header that returns the replenish rate configuration. */
    private String replenishRateHeader = REPLENISH_RATE_HEADER;

    /** The name of the header that returns the burst capacity configuration. */
    private String burstCapacityHeader = BURST_CAPACITY_HEADER;

    private MyRateLimiter.Config defaultConfig;

    protected MyRateLimiter(Class<Config> configClass, String configurationPropertyName, Validator validator) {
        super(configClass, configurationPropertyName, validator);
    }

    public MyRateLimiter(int defaultReplenishRate, int defaultBurstCapacity) {
        super(MyRateLimiter.Config.class, CONFIGURATION_PROPERTY_NAME, null);
        this.defaultConfig = new MyRateLimiter.Config()
                .setReplenishRate(defaultReplenishRate)
                .setBurstCapacity(defaultBurstCapacity);
    }

    public MyRateLimiter(){
        this(1,1);
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        MyRateLimiter.Config routeConfig = getConfig().getOrDefault(routeId, defaultConfig);

        if (routeConfig == null) {
            throw new IllegalArgumentException("No Configuration found for route " + routeId);
        }

        // How many requests per second do you want a user to be allowed to do?
        int replenishRate = routeConfig.getReplenishRate();

        // How much bursting do you want to allow?
        int burstCapacity = routeConfig.getBurstCapacity();

//        try {
//            List<String> keys = getKeys(id);
//
//
//            // The arguments to the LUA script. time() returns unixtime in seconds.
//            List<String> scriptArgs = Arrays.asList(replenishRate + "", burstCapacity + "",
//                    Instant.now().getEpochSecond() + "", "1");
//            // allowed, tokens_left = redis.eval(SCRIPT, keys, args)
//            Flux<List<Long>> flux = this.redisTemplate.execute(this.script, keys, scriptArgs);
//            // .log("redisratelimiter", Level.FINER);
//            return flux.onErrorResume(throwable -> Flux.just(Arrays.asList(1L, -1L)))
//                    .reduce(new ArrayList<Long>(), (longs, l) -> {
//                        longs.addAll(l);
//                        return longs;
//                    }) .map(results -> {
//                        boolean allowed = results.get(0) == 1L;
//                        Long tokensLeft = results.get(1);
//
//                        Response response = new Response(allowed, getHeaders(routeConfig, tokensLeft));
//
//                        if (log.isDebugEnabled()) {
//                            log.debug("response: " + response);
//                        }
//                        return response;
//                    });
//        } catch (Exception e) {
//            /*
//             * We don't want a hard dependency on Redis to allow traffic. Make sure to set
//             * an alert so you know if this is happening too much. Stripe's observed
//             * failure rate is 0.01%.
//             */
//            log.error("Error determining if user allowed from redis", e);
//        }

        return Mono.just(new Response(true, getHeaders(routeConfig, -1L)));
    }

    public HashMap<String, String> getHeaders(MyRateLimiter.Config config, Long tokensLeft) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(this.remainingHeader, tokensLeft.toString());
        headers.put(this.replenishRateHeader, String.valueOf(config.getReplenishRate()));
        headers.put(this.burstCapacityHeader, String.valueOf(config.getBurstCapacity()));
        return headers;
    }

    static List<String> getKeys(String id) {
        // use `{}` around keys to use Redis Key hash tags
        // this allows for using redis cluster

        // Make a unique key per user.
        String prefix = "request_rate_limiter.{" + id;

        // You need two Redis keys for Token Bucket.
        String tokenKey = prefix + "}.tokens";
        String timestampKey = prefix + "}.timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }

    @Validated
    public static class Config {
        @Min(1)
        private int replenishRate;

        @Min(1)
        private int burstCapacity = 1;

        public int getReplenishRate() {
            return replenishRate;
        }

        public MyRateLimiter.Config setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
            return this;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public MyRateLimiter.Config setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
            return this;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "replenishRate=" + replenishRate +
                    ", burstCapacity=" + burstCapacity +
                    '}';
        }
    }
}
