package recruitment.dev.ragservice.dto.client;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillClientDto {

    private Long id;

    private String skillName;

    private Boolean mandatory;
}
