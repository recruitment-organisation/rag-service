package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;

@FeignClient(name = "job-offer-service", fallbackFactory = JobOfferClientFallbackFactory.class)
public interface JobOfferClient {

    @GetMapping("/internal/job-offers/get/{jobOfferId}")
    JobOfferClientDto getJobOfferById(
            @PathVariable("jobOfferId") Long jobOfferId
    );
}
