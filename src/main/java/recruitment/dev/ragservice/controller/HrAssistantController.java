package recruitment.dev.ragservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import recruitment.dev.ragservice.dto.HrAssistantRequest;
import recruitment.dev.ragservice.dto.HrAssistantResponse;
import recruitment.dev.ragservice.service.HrAssistantService;

@RestController
@RequestMapping("/rag/hr/assistant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrAssistantController {

    private final HrAssistantService hrAssistantService;

    @PostMapping("/ask")
    public ResponseEntity<HrAssistantResponse> ask(@Valid @RequestBody HrAssistantRequest request) {
        return ResponseEntity.ok(hrAssistantService.answer(request));
    }
}
