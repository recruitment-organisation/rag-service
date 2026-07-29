package recruitment.dev.ragservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import recruitment.dev.ragservice.client.ApplicationClient;
import recruitment.dev.ragservice.client.JobOfferClient;
import recruitment.dev.ragservice.dto.CvIndexingResult;
import recruitment.dev.ragservice.dto.client.ApplicationClientDto;
import recruitment.dev.ragservice.dto.client.CvClientDto;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;
import recruitment.dev.ragservice.exception.RagException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvIndexingService {

    private final ApplicationClient applicationClient;
    private final JobOfferClient jobOfferClient;
    private final CvStorageService cvStorageService;
    private final PdfExtractorService pdfExtractorService;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder()
            .withChunkSize(550)
            .withMinChunkSizeChars(150)
            .build();

    public CvIndexingResult analyzeCv(
            Long applicationId
    ) {
        long startedAt = System.currentTimeMillis();
        try {
            log.info("Starting RAG analysis: applicationId={}", applicationId);

            ApplicationClientDto application =
                    applicationClient.getApplicationById(applicationId);
            validateApplication(application);

            CvClientDto cv =
                    applicationClient.getCvById(application.getCvId());
            validateCv(cv);

            JobOfferClientDto jobOffer =
                    jobOfferClient.getJobOfferById(application.getJobOfferId());
            validateJobOffer(jobOffer);

            byte[] pdfContent = cvStorageService.downloadCv(cv.getId(), cv);
            String cvText = pdfExtractorService.extractText(pdfContent);

            List<Document> chunks = createChunks(application, cv, cvText);
            replaceIndexedChunks(applicationId, chunks);

            List<Document> relevantChunks = searchRelevantChunks(applicationId, buildJobQuery(jobOffer));
            if (relevantChunks.isEmpty()) {
                throw new RagException(
                        "No relevant CV passage was found for this job offer",
                        "NO_RELEVANT_CV_CONTEXT",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }

            CvIndexingResult result = analyzeWithGemini(jobOffer, relevantChunks);
            validateGeminiResult(result);

            updateMatchingScore(applicationId, result.score());

            log.info(
                    "RAG analysis completed: applicationId={}, chunks={}, retrieved={}, score={}, durationMs={}",
                    applicationId,
                    chunks.size(),
                    relevantChunks.size(),
                    result.score(),
                    System.currentTimeMillis() - startedAt
            );

            return result;
        } catch (FeignException.NotFound exception) {
            throw new RagException(
                    "A required application, CV, or job offer was not found",
                    "REMOTE_RESOURCE_NOT_FOUND",
                    HttpStatus.NOT_FOUND,
                    exception
            );
        } catch (RagException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RagException(
                    "Unable to complete the CV analysis",
                    "RAG_ANALYSIS_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception
            );
        }
    }

    private void validateApplication(
            ApplicationClientDto application
    ) {
        if (application == null) {
            throw new RagException("Application was not found", "APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (application.getId() == null) {
            throw new RagException("Application identifier is missing", "APPLICATION_INVALID", HttpStatus.BAD_REQUEST);
        }

        if (application.getCvId() == null) {
            throw new RagException("No CV was found for this application", "CV_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (application.getCandidateId() == null) {
            throw new RagException("Application candidate is missing", "APPLICATION_INVALID", HttpStatus.BAD_REQUEST);
        }

        if (application.getJobOfferId() == null) {
            throw new RagException("No job offer was found for this application", "JOB_OFFER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }

    private void validateCv(CvClientDto cv) {
        if (cv == null) {
            throw new RagException("CV was not found", "CV_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (Boolean.FALSE.equals(cv.getActive())) {
            throw new RagException("CV is not active", "CV_INACTIVE", HttpStatus.BAD_REQUEST);
        }

        if (cv.getFileType() == null
                || !cv.getFileType()
                .equalsIgnoreCase("application/pdf")) {

            throw new RagException("Only PDF CV files are supported", "CV_FORMAT_UNSUPPORTED", HttpStatus.BAD_REQUEST);
        }

        if (cv.getFileName() == null || cv.getFileName().isBlank()) {
            throw new RagException("CV file name is missing", "CV_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateJobOffer(JobOfferClientDto jobOffer) {
        if (jobOffer == null) {
            throw new RagException("Job offer was not found", "JOB_OFFER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (jobOffer.getId() == null) {
            throw new RagException("Job offer identifier is missing", "JOB_OFFER_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    private List<Document> createChunks(ApplicationClientDto application, CvClientDto cv, String text) {
        List<Document> parts = textSplitter.split(new Document(text));
        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            String chunkText = parts.get(index).getText();
            if (index > 0) {
                chunkText = tail(parts.get(index - 1).getText(), 500) + "\n" + chunkText;
            }
            Map<String, Object> metadata = Map.of(
                    "applicationId", application.getId(),
                    "candidateId", application.getCandidateId(),
                    "jobOfferId", application.getJobOfferId(),
                    "cvId", cv.getId(),
                    "fileName", cv.getFileName(),
                    "documentType", "CV",
                    "chunkIndex", index
            );
            documents.add(Document.builder()
                    .id(documentId(application.getId(), cv.getId(), index))
                    .text(chunkText)
                    .metadata(metadata)
                    .build());
        }
        if (documents.isEmpty()) {
            throw new RagException("CV contains no indexable text", "PDF_TEXT_EMPTY", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return documents;
    }

    private String documentId(Long applicationId, Long cvId, int chunkIndex) {
        String businessKey = applicationId + ":" + cvId + ":" + chunkIndex;
        return UUID.nameUUIDFromBytes(businessKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void replaceIndexedChunks(Long applicationId, List<Document> documents) {
        try {
            vectorStore.delete("applicationId == " + applicationId);
            vectorStore.add(documents);
        } catch (Exception exception) {
            throw new RagException("Unable to index CV passages", "VECTOR_STORE_FAILED", HttpStatus.BAD_GATEWAY, exception);
        }
    }

    private List<Document> searchRelevantChunks(Long applicationId, String query) {
        try {
            return vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(8)
                    .similarityThreshold(0.60)
                    .filterExpression("applicationId == " + applicationId)
                    .build());
        } catch (Exception exception) {
            throw new RagException("Unable to search indexed CV passages", "VECTOR_STORE_FAILED", HttpStatus.BAD_GATEWAY, exception);
        }
    }

    private CvIndexingResult analyzeWithGemini(JobOfferClientDto jobOffer, List<Document> chunks) {
        try {
            CvIndexingResult result = chatClientBuilder.build().prompt()
                    .system("You evaluate job applications. Use only the job offer and CV evidence provided. "
                            + "Never invent skills, experience, durations, or personal facts. Do not use personal data. "
                            + "Every conclusion must be supported by an evidence item copied from the CV context. "
                            + "Return JSON only, with score 0-100, decision REJECTED/REVIEW/RECOMMENDED, summary, "
                            + "matchedSkills, missingMandatorySkills, strengths, weaknesses, evidence, confidence 0-1.")
                    .user(buildGeminiPrompt(jobOffer, chunks))
                    .call()
                    .entity(CvIndexingResult.class, options -> options.validateSchema());
            if (result == null) {
                throw new RagException("Gemini returned an empty analysis", "INVALID_GEMINI_RESPONSE", HttpStatus.BAD_GATEWAY);
            }
            return result;
        } catch (RagException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RagException("Gemini analysis failed", "GEMINI_FAILED", HttpStatus.BAD_GATEWAY, exception);
        }
    }

    private String buildJobQuery(JobOfferClientDto jobOffer) {
        return "Job title: " + safe(jobOffer.getTitle()) + "\nDescription: " + safe(jobOffer.getDescription())
                + "\nExperience level: " + safe(jobOffer.getExperienceLevel())
                + "\nRequirements: " + joinRequirements(jobOffer)
                + "\nSkills: " + joinSkills(jobOffer);
    }

    private String buildGeminiPrompt(JobOfferClientDto jobOffer, List<Document> chunks) {
        String context = chunks.stream().map(Document::getText).collect(Collectors.joining("\n\n--- CV passage ---\n"));
        return "JOB OFFER\n" + buildJobQuery(jobOffer) + "\n\nCV CONTEXT\n" + context;
    }

    private String joinSkills(JobOfferClientDto jobOffer) {
        if (jobOffer.getSkills() == null) return "";
        return jobOffer.getSkills().stream().filter(Objects::nonNull)
                .map(skill -> safe(skill.getSkillName()) + (Boolean.TRUE.equals(skill.getMandatory()) ? " (mandatory)" : ""))
                .collect(Collectors.joining(", "));
    }

    private String joinRequirements(JobOfferClientDto jobOffer) {
        if (jobOffer.getRequirements() == null) return "";
        return jobOffer.getRequirements().stream().filter(Objects::nonNull)
                .map(requirement -> safe(requirement.getRequirement())).collect(Collectors.joining("; "));
    }

    private void validateGeminiResult(CvIndexingResult result) {
        if (result.score() == null || result.score() < 0 || result.score() > 100
                || result.confidence() == null || result.confidence() < 0 || result.confidence() > 1
                || result.decision() == null || !List.of("REJECTED", "REVIEW", "RECOMMENDED").contains(result.decision())
                || result.summary() == null || result.summary().isBlank()
                || result.evidence() == null || result.evidence().isEmpty()) {
            throw new RagException("Gemini returned an invalid structured analysis", "INVALID_GEMINI_RESPONSE", HttpStatus.BAD_GATEWAY);
        }
    }

    private String safe(Object value) { return value == null ? "" : String.valueOf(value); }

    // TokenTextSplitter 2.0 has no overlap option; 500 characters preserve roughly 100 tokens of prior context.
    private String tail(String text, int maxCharacters) {
        return text.length() <= maxCharacters ? text : text.substring(text.length() - maxCharacters);
    }

    private void updateMatchingScore(Long applicationId, Double score) {
        try {
            applicationClient.updateMatchingScore(applicationId,
                    recruitment.dev.ragservice.dto.client.UpdateMatchingScoreRequest.builder().matchingScore(score).build());
        } catch (Exception exception) {
            throw new RagException(
                    "Unable to update the application matching score",
                    "APPLICATION_UPDATE_FAILED",
                    HttpStatus.BAD_GATEWAY,
                    exception
            );
        }
    }
}
