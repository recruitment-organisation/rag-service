package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import recruitment.dev.ragservice.dto.client.ApplicationClientDto;
import recruitment.dev.ragservice.dto.client.CvClientDto;
import recruitment.dev.ragservice.dto.client.UpdateMatchingScoreRequest;

@Component
public class ApplicationClientFallbackFactory implements FallbackFactory<ApplicationClient> {

    @Override
    public ApplicationClient create(Throwable cause) {
        return new ApplicationClient() {
            @Override
            public ApplicationClientDto getApplicationById(Long applicationId) {
                throw RagFeignFallbacks.unavailable("application-service", cause);
            }

            @Override
            public CvClientDto getCvById(Long cvId) {
                throw RagFeignFallbacks.unavailable("application-service", cause);
            }

            @Override
            public byte[] downloadCv(Long cvId) {
                throw RagFeignFallbacks.unavailable("application-service", cause);
            }

            @Override
            public ApplicationClientDto updateMatchingScore(Long applicationId, UpdateMatchingScoreRequest request) {
                throw RagFeignFallbacks.unavailable("application-service", cause);
            }
        };
    }
}
