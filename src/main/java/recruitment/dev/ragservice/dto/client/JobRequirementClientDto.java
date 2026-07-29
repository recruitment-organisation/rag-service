package recruitment.dev.ragservice.dto.client;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirementClientDto {

    private Long id;

    private String requirement;
}
