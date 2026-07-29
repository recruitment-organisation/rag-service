package recruitment.dev.ragservice.dto.client;


import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferClientDto {

    private Long id;

    private String title;

    private String description;

    private String location;

    private String employmentType;

    private String experienceLevel;

    private LocalDate openingDate;

    private LocalDate closingDate;

    private String status;

    private List<JobRequirementClientDto> requirements;

    private List<JobSkillClientDto> skills;
}