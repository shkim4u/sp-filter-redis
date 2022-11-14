package kr.co.starbucks.spfilterredis;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

//    @PostMapping("/load")
//    public String load() {
//        sampleService.loadData();
//        return "Load Data Completed";
//    }

    @PostMapping("/adduser")
    public String adduser() {
        sampleService.addTestUser();
        return "Test User Added";
    }


    @GetMapping("/headers")
    public Map<String, Object> headers(@RequestHeader Map<String, Object> headers) {
        headers.forEach((key, value) -> {
            log.info(String.format("Header '%s' = %s", key, value));
        });
        return headers;
    }

//    @GetMapping("/list")
//    public String list() {
//        sampleService.list();
//        return "List Completed";
//    }

    @GetMapping("/order")
    public String order() {

        return "Order Completed";
    }
}
