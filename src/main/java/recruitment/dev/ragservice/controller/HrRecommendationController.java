package recruitment.dev.ragservice.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recruitment.dev.ragservice.dto.CvIndexingResult;
import recruitment.dev.ragservice.service.CvIndexingService;

@RestController
@RequestMapping("/rag/hr/applications")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('HR')")
public class HrRecommendationController {

    private final CvIndexingService cvIndexingService;

    @PostMapping("/{applicationId}/recommendation")
    public ResponseEntity<CvIndexingResult> recommend(
            @PathVariable @Positive(message = "applicationId must be positive") Long applicationId
    ) {
        return ResponseEntity.ok(cvIndexingService.analyzeCv(applicationId));
    }
}
