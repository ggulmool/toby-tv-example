package com.example.tobythread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

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
        @GetMapping("/callable")
        public Callable<String> callable() throws InterruptedException {
            log.info("callable");
            return () -> {
                // 스프링이 별도의 쓰레드에서 실행해주고 요청 처리한 서블릿 쓰레드는 바로 리턴해준다.
                // 그러면 서블릿쓰레드는 대기할 필요 없다.
                //[nio-8080-exec-1] com.example.tobythread.TobyApplication   : callable
                //[     MvcAsync98] com.example.tobythread.TobyApplication   : callable async
                //[nio-8080-exec-1] com.example.tobythread.TobyApplication   : callable
                //[     MvcAsync99] com.example.tobythread.TobyApplication   : callable async
                //[nio-8080-exec-1] com.example.tobythread.TobyApplication   : callable
                //[    MvcAsync100] com.example.tobythread.TobyApplication   : callable async
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
