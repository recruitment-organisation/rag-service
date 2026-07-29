package recruitment.dev.ragservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import recruitment.dev.ragservice.dto.CvIndexingResult;
import recruitment.dev.ragservice.service.CvIndexingService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequiredArgsConstructor
@Validated
public class CvIndexingController {

    private final CvIndexingService cvIndexingService;

    @PostMapping("/rag/applications/{applicationId}/analyze")
    public ResponseEntity<CvIndexingResult> analyzeCv(
            @PathVariable @Positive(message = "applicationId must be positive") Long applicationId
    ) {
        CvIndexingResult result =
                cvIndexingService.analyzeCv(applicationId);

        return ResponseEntity.ok(result);
    }
}
