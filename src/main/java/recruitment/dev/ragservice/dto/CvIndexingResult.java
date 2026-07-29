package recruitment.dev.ragservice.dto;

import java.util.List;

/** Structured result returned by Gemini after it has received only job data and retrieved CV evidence. */
public record CvIndexingResult(
        Double score,
        String decision,
        String summary,
        List<String> matchedSkills,
        List<String> missingMandatorySkills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> evidence,
        Double confidence
) {
}
