package recruitment.dev.ragservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.mockito.ArgumentCaptor;
import recruitment.dev.ragservice.client.ApplicationClient;
import recruitment.dev.ragservice.client.JobOfferClient;
import recruitment.dev.ragservice.dto.CvIndexingResult;
import recruitment.dev.ragservice.dto.client.ApplicationClientDto;
import recruitment.dev.ragservice.dto.client.CvClientDto;
import recruitment.dev.ragservice.dto.client.JobOfferClientDto;
import recruitment.dev.ragservice.dto.client.JobSkillClientDto;
import recruitment.dev.ragservice.exception.RagException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CvIndexingServiceTest {

    @Test
    void shouldRejectMissingApplication() {
        ApplicationClient applicationClient = mock(ApplicationClient.class);
        CvIndexingService service = service(applicationClient, mock(JobOfferClient.class), mock(CvStorageService.class),
                mock(PdfExtractorService.class), mock(VectorStore.class), mock(ChatClient.Builder.class));

        RagException exception = assertThrows(RagException.class, () -> service.analyzeCv(12L));

        assertEquals("APPLICATION_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenNoRelevantChunkIsFound() {
        ApplicationClient applicationClient = configuredApplicationClient();
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        CvIndexingService service = service(applicationClient, jobOfferClient(), storage(), extractor(), vectorStore,
                mock(ChatClient.Builder.class));

        RagException exception = assertThrows(RagException.class, () -> service.analyzeCv(12L));

        assertEquals("NO_RELEVANT_CV_CONTEXT", exception.getErrorCode());
        verify(vectorStore).delete("applicationId == 12");
    }

    @Test
    void shouldAnalyzeIndexedCvAndFilterSearchByApplicationId() {
        ApplicationClient applicationClient = configuredApplicationClient();
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Java and Spring Boot API development.")));

        ChatClient.Builder builder = chatClientBuilder(new CvIndexingResult(
                82.0, "RECOMMENDED", "Evidence supports the required backend skills.",
                List.of("Java", "Spring Boot"), List.of(), List.of("Relevant API work"), List.of(),
                List.of("Java and Spring Boot API development."), 0.84
        ));

        CvIndexingService service = service(applicationClient, jobOfferClient(), storage(), extractor(), vectorStore, builder);
        CvIndexingResult result = service.analyzeCv(12L);

        assertEquals(82.0, result.score());
        ArgumentCaptor<List<Document>> chunks = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(chunks.capture());
        Document indexedChunk = chunks.getValue().getFirst();
        assertEquals(5L, indexedChunk.getMetadata().get("candidateId"));
        assertEquals(7L, indexedChunk.getMetadata().get("cvId"));
        assertEquals(0, indexedChunk.getMetadata().get("chunkIndex"));
        UUID.fromString(indexedChunk.getId());
        verify(vectorStore).similaritySearch(argThat((SearchRequest request) -> request.toString().contains("applicationId")));
        verify(applicationClient).updateMatchingScore(eq(12L), any());
    }

    private CvIndexingService service(ApplicationClient applicationClient, JobOfferClient jobOfferClient,
                                      CvStorageService storage, PdfExtractorService extractor,
                                      VectorStore vectorStore, ChatClient.Builder builder) {
        return new CvIndexingService(applicationClient, jobOfferClient, storage, extractor, vectorStore, builder);
    }

    private ApplicationClient configuredApplicationClient() {
        ApplicationClient client = mock(ApplicationClient.class);
        when(client.getApplicationById(12L)).thenReturn(ApplicationClientDto.builder()
                .id(12L).candidateId(5L).cvId(7L).jobOfferId(9L).build());
        when(client.getCvById(7L)).thenReturn(CvClientDto.builder()
                .id(7L).fileName("cv.pdf").fileType("application/pdf").active(true).build());
        return client;
    }

    private JobOfferClient jobOfferClient() {
        JobOfferClient client = mock(JobOfferClient.class);
        when(client.getJobOfferById(9L)).thenReturn(JobOfferClientDto.builder().id(9L)
                .title("Backend developer").description("Build Java APIs").experienceLevel("MID")
                .skills(List.of(JobSkillClientDto.builder().skillName("Java").mandatory(true).build()))
                .requirements(List.of()).build());
        return client;
    }

    private CvStorageService storage() {
        CvStorageService storage = mock(CvStorageService.class);
        when(storage.downloadCv(eq(7L), any())).thenReturn(new byte[]{1});
        return storage;
    }

    private PdfExtractorService extractor() {
        PdfExtractorService extractor = mock(PdfExtractorService.class);
        when(extractor.extractText(any())).thenReturn("Java Spring Boot developer building REST APIs. ".repeat(20));
        return extractor;
    }

    private ChatClient.Builder chatClientBuilder(CvIndexingResult result) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenReturn(request);
        when(request.system(anyString())).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.entity(eq(CvIndexingResult.class), any())).thenReturn(result);
        return builder;
    }
}
