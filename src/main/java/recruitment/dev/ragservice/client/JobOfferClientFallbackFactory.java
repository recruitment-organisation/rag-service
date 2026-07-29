package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;

@Component
public class JobOfferClientFallbackFactory implements FallbackFactory<JobOfferClient> {

    @Override
    public JobOfferClient create(Throwable cause) {
        return jobOfferId -> {
            throw RagFeignFallbacks.unavailable("job-offer-service", cause);
        };
    }
}
