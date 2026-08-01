package recruitment.dev.ragservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A question asked from one of the HR list views. */
public record HrAssistantRequest(
        @NotBlank(message = "question must not be blank")
        @Size(max = 1000, message = "question must not exceed 1000 characters")
        String question,
        @NotNull(message = "scope is required")
        HrAssistantScope scope,
        @Min(value = 0, message = "page must be zero or greater")
        Integer page,
        @Min(value = 1, message = "size must be positive")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size
) {
}
