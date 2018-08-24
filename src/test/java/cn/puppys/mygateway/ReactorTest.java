package cn.puppys.mygateway;

import com.google.common.base.Supplier;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Stream;

public class ReactorTest {
    @Test
    public void test1() {
        Integer[] integers = {1, 2, 3, 4, 5};
        Flux<Integer> integerFlux = Flux.fromArray(integers);

        List<Integer> integers1 = Arrays.asList(integers);

        Flux<Integer> integerFlux1 = Flux.fromIterable(integers1);

        Stream<Integer> stream = integers1.stream();
        Flux<Integer> integerFlux2 = Flux.fromStream(stream);

//        integerFlux.subscribe(System.out::println);

        integerFlux.subscribe(System.out::println, System.err::println, () -> System.out.println("打印完了"), subscription -> subscription.request(7));


    }

    @Test
    public void test2() {
        Mono.error(new Exception("some error")).subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("Completed!")
        );
    }

    @Test
    public void test3() {
        Flux.just("flux", "mono")
                .flatMap(s -> Flux.fromArray(s.split("\\s*"))   // 1
                        .delayElements(Duration.ofMillis(100))) // 2
                .doOnNext(System.out::print).subscribe(System.out::println);
    }

    @Test
    public void test4() {
        String s = "sss";
        Flux<String> stringFlux = Flux.fromArray(s.split("\\s*"))   // 1
                .delayElements(Duration.ofMillis(100));
    }



    @Test
    public void testSimpleOperators() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);  // 2
        Flux.zip(
                getZipDescFlux(),
                Flux.interval(Duration.ofMillis(200)))  // 3
                .subscribe(System.out::println, null, countDownLatch::countDown);    // 4
        countDownLatch.await(10, TimeUnit.SECONDS);     // 5
    }

    private static Flux<String> getZipDescFlux() {
        String desc = "Zip two sources together, that is to say wait for all the sources to emit one element and combine these elements once into a Tuple2.";
        return Flux.fromArray(desc.split("\\s+"));  // 1
    }

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);  // 2
        System.out.println("======"+Thread.currentThread().getName());
        Flux.zip(
                getZipDescFlux(),
                Flux.interval(Duration.ofMillis(200)))  // 3
                .subscribe(objects -> {
                    System.out.println(objects.getClass().getName());
                    System.out.println("----"+Thread.currentThread().getName());
                }, null, () ->{
                    System.out.println("finished Thread name "+Thread.currentThread().getName());
                    countDownLatch.countDown();
                });    // 4
        System.out.println("main go to sleep");
        Thread.sleep(8000L);
        System.out.println("main wake up");
        countDownLatch.await(20, TimeUnit.SECONDS);     // 5
    }

    @Test
    public void test5(){
        Integer[] arr = {1, 2, 3, 4, 5};
        List<Integer> integers = Arrays.asList(arr);

    }

    @Test
    public void test6() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(1);

        new Thread(atomicInteger::incrementAndGet).start();

        Thread.sleep(100L);

        System.out.println(atomicInteger.get());
    }
}
