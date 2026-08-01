package recruitment.dev.ragservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import recruitment.dev.ragservice.client.ApplicationClient;
import recruitment.dev.ragservice.client.CandidateClient;
import recruitment.dev.ragservice.client.JobOfferClient;
import recruitment.dev.ragservice.dto.HrAssistantRequest;
import recruitment.dev.ragservice.dto.HrAssistantResponse;
import recruitment.dev.ragservice.dto.HrAssistantScope;
import recruitment.dev.ragservice.dto.client.ApplicationClientDto;
import recruitment.dev.ragservice.dto.client.CandidateClientDto;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;
import recruitment.dev.ragservice.dto.client.PageResult;
import recruitment.dev.ragservice.exception.RagException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class HrAssistantService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_FIELD_LENGTH = 500;

    private final ApplicationClient applicationClient;
    private final CandidateClient candidateClient;
    private final JobOfferClient jobOfferClient;
    private final ChatClient.Builder chatClientBuilder;

    public HrAssistantResponse answer(HrAssistantRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();

        List<ApplicationClientDto> applications = content(applicationClient.getApplications(
                request.scope() == HrAssistantScope.APPLICATIONS ? page : 0,
                request.scope() == HrAssistantScope.APPLICATIONS ? size : 100
        ));
        List<CandidateClientDto> candidates = content(candidateClient.getCandidates(
                request.scope() == HrAssistantScope.CANDIDATES ? page : 0,
                request.scope() == HrAssistantScope.CANDIDATES ? size : 100
        ));
        List<JobOfferClientDto> jobOffers = content(jobOfferClient.getJobOffers(
                request.scope() == HrAssistantScope.JOB_OFFERS ? page : 0,
                request.scope() == HrAssistantScope.JOB_OFFERS ? size : 100
        ));

        HrAssistantResponse generated = askModel(request, applications, candidates, jobOffers);
        validateAnswer(generated);

        Set<Long> allowedIds = idsForScope(request.scope(), applications, candidates, jobOffers);
        List<Long> matchingIds = generated.filterApplied() ? generated.matchingIds().stream()
                .filter(Objects::nonNull)
                .filter(allowedIds::contains)
                .distinct()
                .toList() : List.of();

        return new HrAssistantResponse(
                generated.answer().trim(),
                generated.filterApplied(),
                matchingIds,
                generated.followUpQuestions().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(question -> !question.isBlank())
                        .limit(3)
                        .toList()
        );
    }

    private HrAssistantResponse askModel(
            HrAssistantRequest request,
            List<ApplicationClientDto> applications,
            List<CandidateClientDto> candidates,
            List<JobOfferClientDto> jobOffers
    ) {
        try {
            HrAssistantResponse result = chatClientBuilder.build().prompt()
                    .system(systemPrompt(request.scope()))
                    .user(buildPrompt(request.question(), request.scope(), applications, candidates, jobOffers))
                    .call()
                    .entity(HrAssistantResponse.class, options -> options.validateSchema());
            if (result == null) {
                throw new RagException("The HR assistant returned an empty response", "INVALID_ASSISTANT_RESPONSE", HttpStatus.BAD_GATEWAY);
            }
            return result;
        } catch (RagException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RagException(
                    "The HR assistant is temporarily unavailable",
                    "HR_ASSISTANT_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY,
                    exception
            );
        }
    }

    private String systemPrompt(HrAssistantScope scope) {
        return "You are a French HR decision-support assistant. Answer only from the supplied records; "
                + "never invent a fact, skill, experience, status, or score. Do not expose emails, phone numbers, "
                + "or other contact details. A matching score or recommendation is decision support, never an automatic hiring decision. "
                + "The active filter scope is " + scope + ". Return French JSON matching this schema. "
                + "Set filterApplied to true only when the user asks to list, select, rank, or filter records in the active scope. "
                + "When filterApplied is true, matchingIds must contain only IDs from that active scope and only records supported by the data. "
                + "When the question is informational, set filterApplied to false and matchingIds to an empty list. "
                + "Keep the answer concise, neutral, and explain any limitation in the available data. "
                + "Provide at most three useful followUpQuestions.";
    }

    private String buildPrompt(
            String question,
            HrAssistantScope scope,
            List<ApplicationClientDto> applications,
            List<CandidateClientDto> candidates,
            List<JobOfferClientDto> jobOffers
    ) {
        Map<Long, CandidateClientDto> candidatesById = byId(candidates, CandidateClientDto::getId);
        Map<Long, JobOfferClientDto> offersById = byId(jobOffers, JobOfferClientDto::getId);

        StringBuilder context = new StringBuilder();
        context.append("ACTIVE SCOPE: ").append(scope).append("\n");
        context.append("QUESTION: ").append(clean(question)).append("\n\n");

        context.append("APPLICATIONS\n");
        if (applications.isEmpty()) {
            context.append("No application is available on this page.\n");
        } else {
            for (ApplicationClientDto application : applications) {
                CandidateClientDto candidate = candidatesById.get(application.getCandidateId());
                JobOfferClientDto offer = offersById.get(application.getJobOfferId());
                context.append("- id=").append(application.getId())
                        .append(" | candidate=").append(candidateLabel(application.getCandidateId(), candidate))
                        .append(" | offer=").append(offerLabel(application.getJobOfferId(), offer))
                        .append(" | status=").append(clean(application.getStatus()))
                        .append(" | step=").append(clean(application.getCurrentStep()))
                        .append(" | matchingScore=").append(application.getMatchingScore() == null ? "not available" : application.getMatchingScore())
                        .append("\n");
            }
        }

        context.append("\nCANDIDATES\n");
        if (candidates.isEmpty()) {
            context.append("No candidate is available on this page.\n");
        } else {
            for (CandidateClientDto candidate : candidates) {
                context.append("- id=").append(candidate.getId())
                        .append(" | name=").append(candidateLabel(candidate.getId(), candidate))
                        .append(" | location=").append(clean(candidate.getLocation()))
                        .append(" | available=").append(candidate.getAvailable() == null ? "not available" : candidate.getAvailable())
                        .append("\n");
            }
        }

        context.append("\nJOB OFFERS\n");
        if (jobOffers.isEmpty()) {
            context.append("No job offer is available on this page.\n");
        } else {
            for (JobOfferClientDto offer : jobOffers) {
                context.append("- id=").append(offer.getId())
                        .append(" | title=").append(clean(offer.getTitle()))
                        .append(" | status=").append(clean(offer.getStatus()))
                        .append(" | location=").append(clean(offer.getLocation()))
                        .append(" | employmentType=").append(clean(offer.getEmploymentType()))
                        .append(" | experienceLevel=").append(clean(offer.getExperienceLevel()))
                        .append(" | skills=").append(skills(offer))
                        .append(" | requirements=").append(requirements(offer))
                        .append("\n");
            }
        }
        return context.toString();
    }

    private void validateAnswer(HrAssistantResponse result) {
        if (result.answer() == null || result.answer().isBlank()) {
            throw new RagException("The HR assistant returned an invalid response", "INVALID_ASSISTANT_RESPONSE", HttpStatus.BAD_GATEWAY);
        }
    }

    private Set<Long> idsForScope(
            HrAssistantScope scope,
            List<ApplicationClientDto> applications,
            List<CandidateClientDto> candidates,
            List<JobOfferClientDto> jobOffers
    ) {
        return switch (scope) {
            case APPLICATIONS -> ids(applications, ApplicationClientDto::getId);
            case CANDIDATES -> ids(candidates, CandidateClientDto::getId);
            case JOB_OFFERS -> ids(jobOffers, JobOfferClientDto::getId);
        };
    }

    private String candidateLabel(Long candidateId, CandidateClientDto candidate) {
        if (candidate == null) return "candidate #" + candidateId;
        String fullName = (clean(candidate.getFirstName()) + " " + clean(candidate.getLastName())).trim();
        return fullName.isBlank() ? "candidate #" + candidateId : fullName;
    }

    private String offerLabel(Long jobOfferId, JobOfferClientDto offer) {
        return offer == null || clean(offer.getTitle()).isBlank() ? "job offer #" + jobOfferId : clean(offer.getTitle());
    }

    private String skills(JobOfferClientDto offer) {
        if (offer.getSkills() == null) return "";
        return offer.getSkills().stream()
                .filter(Objects::nonNull)
                .map(skill -> clean(skill.getSkillName()) + (Boolean.TRUE.equals(skill.getMandatory()) ? " (mandatory)" : ""))
                .collect(Collectors.joining(", "));
    }

    private String requirements(JobOfferClientDto offer) {
        if (offer.getRequirements() == null) return "";
        return offer.getRequirements().stream()
                .filter(Objects::nonNull)
                .map(requirement -> clean(requirement.getRequirement()))
                .collect(Collectors.joining("; "));
    }

    private String clean(String value) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_FIELD_LENGTH ? normalized : normalized.substring(0, MAX_FIELD_LENGTH) + "…";
    }

    private <T> List<T> content(PageResult<T> page) {
        return page == null || page.content() == null ? List.of() : page.content();
    }

    private <T> Map<Long, T> byId(Collection<T> values, Function<T, Long> idExtractor) {
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> idExtractor.apply(value) != null)
                .collect(Collectors.toMap(idExtractor, Function.identity(), (first, ignored) -> first));
    }

    private <T> Set<Long> ids(Collection<T> values, Function<T, Long> idExtractor) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
