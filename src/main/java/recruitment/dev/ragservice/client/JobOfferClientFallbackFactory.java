package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;
import recruitment.dev.ragservice.dto.client.PageResult;

@Component
public class JobOfferClientFallbackFactory implements FallbackFactory<JobOfferClient> {

    @Override
    public JobOfferClient create(Throwable cause) {
        return new JobOfferClient() {
            @Override
            public PageResult<JobOfferClientDto> getJobOffers(int page, int size) {
                throw RagFeignFallbacks.unavailable("job-offer-service", cause);
            }

            @Override
            public JobOfferClientDto getJobOfferById(Long jobOfferId) {
                throw RagFeignFallbacks.unavailable("job-offer-service", cause);
            }
        };
    }
}
