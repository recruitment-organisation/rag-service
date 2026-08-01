package recruitment.dev.ragservice.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateClientDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String location;
    private Boolean available;
}
