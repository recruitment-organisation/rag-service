package recruitment.dev.ragservice.dto.client;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvClientDto {

    private Long id;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Boolean active;

    private LocalDateTime uploadedAt;
}