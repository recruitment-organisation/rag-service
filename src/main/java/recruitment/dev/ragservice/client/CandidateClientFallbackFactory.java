package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.ragservice.dto.client.CandidateClientDto;
import recruitment.dev.ragservice.dto.client.PageResult;

@Component
public class CandidateClientFallbackFactory implements FallbackFactory<CandidateClient> {

    @Override
    public CandidateClient create(Throwable cause) {
        return new CandidateClient() {
            @Override
            public PageResult<CandidateClientDto> getCandidates(int page, int size) {
                throw RagFeignFallbacks.unavailable("candidate-service", cause);
            }
        };
    }
}
