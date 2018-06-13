package me.ggulmool.ch05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MyApplication {

    @RestController
    public static class MyController {
        //RestTemplate rt = new RestTemplate();
        AsyncRestTemplate rt = new AsyncRestTemplate();

//        @GetMapping("/rest")
//        public String rest(int idx) {
//            String res = rt.getForObject("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
//            return res;
//        }

        @GetMapping("/rest")
        public ListenableFuture<ResponseEntity<String>> rest(int idx) {
            // api호출시 즉시 리턴하고 결과가 오면 callback 함수 실행한다.
            return rt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
