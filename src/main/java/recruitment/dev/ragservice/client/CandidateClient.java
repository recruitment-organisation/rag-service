package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import recruitment.dev.ragservice.dto.client.CandidateClientDto;
import recruitment.dev.ragservice.dto.client.PageResult;

@FeignClient(name = "candidate-service", fallbackFactory = CandidateClientFallbackFactory.class)
public interface CandidateClient {

    @GetMapping("/candidate/get-all")
    PageResult<CandidateClientDto> getCandidates(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}
