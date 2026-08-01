package recruitment.dev.ragservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HrAssistantServiceTest {

    @Test
    void shouldKeepOnlyApplicationIdsPresentInTheVisiblePage() {
        ApplicationClient applications = mock(ApplicationClient.class);
        CandidateClient candidates = mock(CandidateClient.class);
        JobOfferClient offers = mock(JobOfferClient.class);
        when(applications.getApplications(2, 50)).thenReturn(new PageResult<>(List.of(
                ApplicationClientDto.builder().id(8L).candidateId(3L).jobOfferId(5L).status("SUBMITTED").matchingScore(81.0).build()
        )));
        when(candidates.getCandidates(0, 100)).thenReturn(new PageResult<>(List.of(
                CandidateClientDto.builder().id(3L).firstName("Amira").lastName("Ben Salem").location("Tunis").available(true).build()
        )));
        when(offers.getJobOffers(0, 100)).thenReturn(new PageResult<>(List.of(
                JobOfferClientDto.builder().id(5L).title("Backend Engineer").status("OPEN").build()
        )));

        HrAssistantService service = new HrAssistantService(applications, candidates, offers,
                chatClientBuilder(new HrAssistantResponse(
                        "La candidature #8 présente un score de 81/100.", true, List.of(8L, 999L), List.of("Quels éléments faut-il vérifier ?")
                )));

        HrAssistantResponse result = service.answer(new HrAssistantRequest(
                "Affiche les candidatures recommandées", HrAssistantScope.APPLICATIONS, 2, 50
        ));

        assertTrue(result.filterApplied());
        assertEquals(List.of(8L), result.matchingIds());
        verify(applications).getApplications(2, 50);
    }

    @Test
    void shouldKeepInformationalAnswersUnfiltered() {
        ApplicationClient applications = mock(ApplicationClient.class);
        CandidateClient candidates = mock(CandidateClient.class);
        JobOfferClient offers = mock(JobOfferClient.class);
        when(applications.getApplications(0, 100)).thenReturn(new PageResult<>(List.of()));
        when(candidates.getCandidates(0, 20)).thenReturn(new PageResult<>(List.of(
                CandidateClientDto.builder().id(3L).firstName("Amira").lastName("Ben Salem").available(true).build()
        )));
        when(offers.getJobOffers(0, 100)).thenReturn(new PageResult<>(List.of()));

        HrAssistantService service = new HrAssistantService(applications, candidates, offers,
                chatClientBuilder(new HrAssistantResponse("Le profil est marqué disponible.", false, List.of(3L), List.of())));

        HrAssistantResponse result = service.answer(new HrAssistantRequest(
                "Le candidat est-il disponible ?", HrAssistantScope.CANDIDATES, 0, 20
        ));

        assertFalse(result.filterApplied());
        assertEquals(List.of(), result.matchingIds());
    }

    private ChatClient.Builder chatClientBuilder(HrAssistantResponse result) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenReturn(request);
        when(request.system(anyString())).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.entity(eq(HrAssistantResponse.class), any())).thenReturn(result);
        return builder;
    }
}
