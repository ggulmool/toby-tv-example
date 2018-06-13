package com.example.tobythread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@SpringBootApplication
@Slf4j
@EnableAsync
public class TobyApplication {

    // 1. 결과 -> 데이터베이스에 넣는다. db를 계속 바라보고 select에 결과가 있으면 결과값 가져온다.
    // 2. HttpSession에 future객체를 저장하고 session에서 future를 꺼내 f.isDone이 true이면 결과값 가져온다.
    @Component
    public static class MyService {
        @Async
        public Future<String> hello() throws InterruptedException {
            log.info("hello()");
            Thread.sleep(2000);
            return new AsyncResult<>("hello");
        }
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext c = SpringApplication.run(TobyApplication.class, args)) {

        }
    }

    @Autowired MyService myService;

    @Bean
    ApplicationRunner run() {
        return args -> {
            log.info("run()");
            Future<String> f = myService.hello();
            log.info("exit : " + f.isDone());
            log.info("result: " + f.get());
        };
    }
}
