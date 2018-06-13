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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@SpringBootApplication
@Slf4j
@EnableAsync
public class TobyApplication {

    @Component
    public static class MyService {
        @Async(value = "tp")
        public ListenableFuture<String> hello() throws InterruptedException {
            log.info("hello()");
            Thread.sleep(2000);
            return new AsyncResult<>("hello");
        }
    }

    // 첫번째 쓰레드 요청이 오면 core사이즈 만큼 쓰레드를 만들어둔다.
    // core사이즈가 다 차면 -> 큐에 요청 쌓이고 -> 큐 요청사이즈가 다차면-> maxpool사이즈만큼 새로 쓰레드 생성한다.
    @Bean
    ThreadPoolTaskExecutor tp() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(10);
        te.setMaxPoolSize(100);
        te.setQueueCapacity(200);
        // 쓰레드 실행 앞뒤로 현재 쓰레드풀의 상태를 로그로 남길때 사용한다.
        //te.setTaskDecorator();
        te.setThreadNamePrefix("myThread");
        te.initialize();
        return te;
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
            ListenableFuture<String> f = myService.hello();
            f.addCallback(s -> System.out.println(s), e -> System.out.println(e.getMessage()));
            log.info("exit");
        };
    }
}
