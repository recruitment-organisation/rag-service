package recruitment.dev.ragservice.dto.client;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationClientDto {

    private Long id;

    private Long candidateId;

    private Long jobOfferId;

    private Long cvId;

    private String status;

    private String currentStep;

    private Double matchingScore;
}