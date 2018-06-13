package me.ggulmool.ch04;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
public class FutureEx {

    interface SuccessCallback {
        void onSuccess(String result);
    }

    interface ExceptionCallback {
        void onError(Throwable t);
    }

    static class CallbackFutureTask extends FutureTask<String> {
        SuccessCallback sc;
        ExceptionCallback ec;
        public CallbackFutureTask(Callable<String> callable, SuccessCallback sc, ExceptionCallback ec) {
            super(callable);
            this.sc = Objects.requireNonNull(sc);
            this.ec = Objects.requireNonNull(ec);
        }

        @Override
        protected void done() {
            try {
                sc.onSuccess(get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                ec.onError(e.getCause());
            }
        }
    }

    // 이 방식은 비동기 작업을 수행하는 코드와 쓰레드풀 생성과 종료하는 코드가 한군데 혼재해 있다.
    // 성격이 다른 기술적인 코드와 비지니스 적인 코드가 한군데 혼재해 있다.
    // 깔끔하게 분리하고 추상화 해준 스프링을 사용하여 개선.
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newCachedThreadPool();

        CallbackFutureTask f = new CallbackFutureTask(() -> {
            Thread.sleep(2000);
            if (1 == 1) throw new RuntimeException("Async ERROR!!");
            log.info("Async");
            return "Hello";
        },
                r -> System.out.println(r),
                e -> System.out.println(e.getMessage()));

        es.execute(f);
        es.shutdown();
    }
}
