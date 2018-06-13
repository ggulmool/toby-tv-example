package me.ggulmool.ch04;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

/*
ex) 블로킹 IO작업 때문에 쓰레드 풀이 꽉차면 그 이후 요청은 큐에서 요청 대기하기 때문에 매우 비효율
실제적으로는 blocking된 쓰레드들은 놀고 있는데 쓰레드 풀이 꽉차 버렸기 때문에 응답 지연
ST1 - req -> blocking IO(DB, API) -> res(html)
ST2 - req -> blocking IO(DB, API) -> res(html)
ST3 - req -> blocking IO(DB, API) -> res(html)
ST4 - req -> blocking IO(DB, API) -> res(html)

ex) 쓰레드 풀을 늘리는 경우
기본적으로 쓰레드 하나가 스택트레이스가 자기 데이터를 가지고 있어야 되기 때문에 메모리를 많이 잡는다.
쓰레드를 많이 만들면 out of memory 에러 발생.
cpu가 컨텍스트 스위칭에 많은 자원을 사용하기 때문에 cpu에 부하가 걸리기 때문에 나중에는 쓰레드를 적게 잡앗을때보다 처리율이 떨어진다.

이를 해결하기 나온게 Servlet3.0: 비동기 서블릿 기술이다.
- HTTP Connection은 이미 논블로킹 IO
- 서블릿 요청 읽기, 응답 쓰기는 블로킹
- 비동기 작업 시작 즉시 서블릿 쓰레드 반납
- 비동기 작업이 완료되면 서블릿 쓰레드 재할당
- 비동기 서블릿 컨텍스트 이용(AsyncContext)
Servlet3.1: 논블로킹 IO
- 논블로킹 서블릿 요청, 응답 처리
- Callback
 */
@SpringBootApplication
@Slf4j
@EnableAsync
public class TobyApplication {

    @RestController
    public static class MyContoller {
        Queue<DeferredResult<String>> results = new ConcurrentLinkedQueue<>();

        // 데이터를 여러번에 나눠서 보낼때 사용
        @GetMapping("/emitter")
        public ResponseBodyEmitter emitter() {
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();

            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    for (int i = 0; i <= 50; i++) {
                        emitter.send("<p>Stream " + i + "</p>");
                        Thread.sleep(500);
                    }
                } catch (Exception e) {}
            });
            return emitter;
        }

        @GetMapping("/dr")
        public DeferredResult<String> deferredResult() {
            log.info("dr");
            // DeferedResult는 setResult, setError 호출되기 전까지 http요청에 대한 응답을 대기하고 있다. 서블릿 쓰레드는 반납
            // 최대 장점은 Worker 쓰레드를 따로 만들지 않기 때문에 서블릿 자원을 최소한으로 활용하면서 동시에 수많은 요청을 처리할 수 있다.
            // 쓰레드 하나로 처리.
            // 이벤트성 구조인 경우에 유용. 비동기 io를 이용한 외부 io를 호출할 때도 사용
            DeferredResult<String> dr = new DeferredResult<>(600000L);
            results.add(dr);
            return dr;
        }

        @GetMapping("/dr/count")
        public String drcount() {
            return String.valueOf(results.size());
        }

        @GetMapping("/dr/event")
        public String drevent(String msg) {
            for (DeferredResult<String> dr : results) {
                // 이벤트 결과를 쓰는 순간 응답한다.
                dr.setResult("Hello " + msg);
                results.remove(dr);
            }
            return "OK";
        }

        @GetMapping("/callable")
        public Callable<String> callable() throws InterruptedException {
            log.info("callable");
            return () -> {
                // 스프링이 별도의 쓰레드에서 실행해주고 요청 처리한 서블릿 쓰레드는 바로 리턴해준다.
                // 그러면 서블릿쓰레드는 대기할 필요 없다.
                //[nio-8080-exec-1] me.ggulmool.ch04.TobyApplication   : callable
                //[     MvcAsync98] me.ggulmool.ch04.TobyApplication   : callable async
                //[nio-8080-exec-1] me.ggulmool.ch04.TobyApplication   : callable
                //[     MvcAsync99] me.ggulmool.ch04.TobyApplication   : callable async
                //[nio-8080-exec-1] me.ggulmool.ch04.TobyApplication   : callable
                //[    MvcAsync100] me.ggulmool.ch04.TobyApplication   : callable async
                log.info("callable async");
                Thread.sleep(2000);
                return "hello";
            };
        }

        @GetMapping("/async")
        public String test() throws InterruptedException {
            log.info("async");
            Thread.sleep(2000);
            return "hello";
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(TobyApplication.class, args);
    }
}
