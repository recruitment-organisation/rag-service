package recruitment.dev.ragservice.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import recruitment.dev.ragservice.client.ApplicationClient;
import recruitment.dev.ragservice.dto.client.CvClientDto;
import recruitment.dev.ragservice.exception.RagException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvStorageServiceImpl implements CvStorageService {

    private final MinioClient minioClient;
    private final ApplicationClient applicationClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public byte[] downloadCv(Long cvId, CvClientDto cv) {
        if (cvId == null) {
            throw new RagException("CV identifier is required", "CV_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (cv == null) {
            throw new RagException("CV details are required", "CV_INVALID", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        String objectName = resolveObjectName(cv.getFileUrl());

        if (objectName != null) {
            try {
                return downloadFromMinio(objectName);
            } catch (Exception exception) {
                log.warn(
                        "MinIO download failed for cvId={}, fallback to application-service",
                        cvId,
                        exception
                );
            }
        }

        return downloadFromApplicationService(cvId);
    }

    private byte[] downloadFromMinio(@NonNull String objectName) {
        try (
                GetObjectResponse response =
                        minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .build()
                        )
        ) {
            return response.readAllBytes();

        } catch (Exception exception) {
            throw new RagException(
                "Unable to download the CV from storage",
                "CV_DOWNLOAD_FAILED",
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                exception
            );
        }
    }

    private byte[] downloadFromApplicationService(Long cvId) {
        try {
            log.info("Downloading CV from application-service for cvId={}", cvId);
            byte[] content = applicationClient.downloadCv(cvId);

            if (content == null || content.length == 0) {
                throw new RagException("Downloaded CV file is empty", "PDF_EMPTY", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
            }

            return content;
        } catch (Exception exception) {
            throw new RagException(
                "Unable to download the CV from application-service",
                "CV_DOWNLOAD_FAILED",
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                exception
            );
        }
    }

    private String resolveObjectName(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        String bucketPart = "/" + bucketName + "/";
        int bucketPosition = fileUrl.indexOf(bucketPart);

        if (bucketPosition >= 0) {
            return fileUrl.substring(bucketPosition + bucketPart.length());
        }

        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            return fileUrl;
        }

        return null;
    }
}
