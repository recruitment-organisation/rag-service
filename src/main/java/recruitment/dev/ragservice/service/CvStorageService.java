package recruitment.dev.ragservice.service;

import recruitment.dev.ragservice.dto.client.CvClientDto;

public interface CvStorageService {

    byte[] downloadCv(Long cvId, CvClientDto cv);
}
