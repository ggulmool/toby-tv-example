### 토비님 방송 시청

#### 스프링 리액티브 프로그래밍 (4) [자바와 스프링의 비동기 기술](https://www.youtube.com/watch?v=aSTuQiPB4Ns&t=4916s)
- 순수 자바 쓰레드를 활용하여 비동기 처리를 하기 위한 Future,FutureTask를 활용한 콜백방식
    - 이 방식은 비동기 작업을 수행하는 코드와 쓰레드풀 생성과 종료하는 코드가 한군데 혼재해 있다.
    - 성격이 다른 기술적인 코드와 비지니스 적인 코드가 한군데 혼재해 있다.
    - 깔끔하게 분리하고 추상화 해준 스프링을 사용하여 개선.
- 기본적인 Spring의 비동기 방식 @EnableAsync, @Async 호출
- Spring의 비동기 callback 방식 처리를 위한 ListenableFuturue 활용
    - SuccessCallback과 FailCallback을 지정할 수 있다.
- 스프링의 ThreadPoolTaskExecutor의 쓰레딩 정책, 갯수 설정
    - 첫번째 쓰레드 요청이 오면 core사이즈 만큼 쓰레드를 만들어둔다.
    - core사이즈가 다 차면 -> 큐에 요청 쌓이고 -> 큐 요청사이즈가 다차면-> maxpool사이즈만큼 새로 쓰레드 생성
    
```
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
```

- 스프링 비동기 서블릿 기술 Callable 처리 
    - Worker 쓰레드에 작업 요청 하고 서블릿 쓰레드는 바로 반환
- 스프링 비동기 서블릿 기술 DeferedResult 처리 
    - DeferedResult는 setResult, setError 호출되기 전까지 http요청에 대한 응답을 대기하고 있다. 서블릿 쓰레드는 반납
    - 최대 장점은 Worker 쓰레드를 따로 만들지 않기 때문에 서블릿 자원을 최소한으로 활용하면서 동시에 수많은 요청을 처리할 수 있다.
    - 쓰레드 하나로 처리.
    - 이벤트성 구조인 경우에 유용. 비동기 io를 이용한 외부 io를 호출할 때도 사용
- 스프링 비동기 서블릿 기술 Emitter
    - 데이터를 여러번에 나눠서 보낼때 사용


    