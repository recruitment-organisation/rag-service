package recruitment.dev.ragservice.dto;

import java.util.List;

/** Grounded answer and, when requested, IDs that the current HR view can filter on. */
public record HrAssistantResponse(
        String answer,
        boolean filterApplied,
        List<Long> matchingIds,
        List<String> followUpQuestions
) {
    public HrAssistantResponse {
        matchingIds = matchingIds == null ? List.of() : List.copyOf(matchingIds);
        followUpQuestions = followUpQuestions == null ? List.of() : List.copyOf(followUpQuestions);
    }
}
