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

#### 스프링 리액티브 프로그래밍 (5) [비동기 RestTemplate과 비동기 MVC/Servlet](https://www.youtube.com/watch?v=ExUfZkh7Puk)
- ThreadPool Hell
    - 쓰레드 수를 많이 잡는다고 무조건 좋은건 아니다.
    - 쓰레드 수가 많으면 컨텍스트 스위칭이 빈번하게 발생
    - 컨텍스트 스위칭도 CPU의 자원을 많이 점유한다. 외부의 API 호출은 블로킹 상태가 되는데 그럴때마다 컨텍스트 스위칭한다.
    
- CyclicBarrier
    - 설정한 barrier갯수 만큼 await() 블로킹 시키고 barrier값에 만족하면 await() 이후의 로직을 동시에 수행 시킨다.

- RestTemplate
    - getForObject() 블로킹 메소드
    
- AsyncRestTemplate
    - getForEntity() -> ListenableFuture 리턴
    - 결과값이 오면 처리하는 callback은 SpringMVC 알아서 callback 등록하기 때문에 callback만들 필요 없다.
    - 하지만 AsyncRestTemplate는 비동기로 작업을 처리하기 위해서 background에 worker 쓰레드를 만들어서 실행한다.
    - 새로운 쓰레드를 새로 만들어서 자원을 추가로 사용하기 때문에 비효율

- AsyncRestTemplate 논블로킹 방식
    - NettyEventGroup 추가
    - 쓰레드를 추가로 만들지 않고 하나의 쓰레드로 다 처리

- ListenableFuture
    - Controller에서 Listenable리턴시 Spring에 의해 미리 정의된 callback함수 실행
    - api 호출 후 리턴 받은 값을 가공해서 비동기로 리턴하려면 DeferedResult 이용.
        - ListenableFuture에 addCallback 메소드에 호출될 콜백 메소드 지정해서 리턴받은 값 가공한다.
        - DeferedResult에 setResult하는 순간 가공처리한 값을 리턴한다.
    - Controller메소드는 즉시 리턴하고 백그라운드에서 논블로킹 방식으로 비동기 처리하기 때문에 많은 외부 서비스를 처리하는 것들이 가능하다.
    - 그러나 코드가 콜백 헬 코드
```
@GetMapping("/rest")
public DeferredResult<String> rest(int idx) {
    DeferredResult<String> dr = new DeferredResult<>();

    ListenableFuture<ResponseEntity<String>> f1 = rt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
    f1.addCallback(s -> {
        ListenableFuture<ResponseEntity<String>> f2 = rt.getForEntity("http://localhost:8081/service2?req={req}", String.class, s.getBody());
        f2.addCallback(s2 -> {
            ListenableFuture<String> f3 = myService.work(s2.getBody());
            f3.addCallback(s3 -> {
                dr.setResult(s3);
            }, e -> {
                dr.setErrorResult(e.getMessage());
            });
        }, e -> {
            dr.setErrorResult(e.getMessage());
        });
    }, e -> {
        dr.setErrorResult(e.getMessage());
    });
    return dr;
}  
    
```