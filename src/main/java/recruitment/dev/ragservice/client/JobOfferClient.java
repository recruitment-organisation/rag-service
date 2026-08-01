package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;
import recruitment.dev.ragservice.dto.client.PageResult;

@FeignClient(name = "job-offer-service", fallbackFactory = JobOfferClientFallbackFactory.class)
public interface JobOfferClient {

    @GetMapping("/job-offers/getall")
    PageResult<JobOfferClientDto> getJobOffers(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @GetMapping("/internal/job-offers/get/{jobOfferId}")
    JobOfferClientDto getJobOfferById(
            @PathVariable("jobOfferId") Long jobOfferId
    );
}
