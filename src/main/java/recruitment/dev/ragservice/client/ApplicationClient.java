package recruitment.dev.ragservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import recruitment.dev.ragservice.dto.client.ApplicationClientDto;
import recruitment.dev.ragservice.dto.client.CvClientDto;
import recruitment.dev.ragservice.dto.client.UpdateMatchingScoreRequest;

@FeignClient(
        name = "application-service",
        fallbackFactory = ApplicationClientFallbackFactory.class
)
public interface ApplicationClient {

    @GetMapping("/internal/applications/{applicationId}")
    ApplicationClientDto getApplicationById(
            @PathVariable("applicationId")
            Long applicationId
    );

    @GetMapping("/internal/cv/{cvId}")
    CvClientDto getCvById(
            @PathVariable("cvId")
            Long cvId
    );

    @GetMapping("/internal/cv/{cvId}/download")
    byte[] downloadCv(
            @PathVariable("cvId")
            Long cvId
    );

    @PutMapping("/internal/applications/{applicationId}/matching-score")
    ApplicationClientDto updateMatchingScore(
            @PathVariable("applicationId") Long applicationId,
            @RequestBody UpdateMatchingScoreRequest request
    );
}
