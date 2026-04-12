package sonmoeum.domain.file.service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsStorageService implements StorageService {

    private static final String GCS_URL_PREFIX = "https://storage.googleapis.com/";

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket:test-bucket}")
    private String bucketName;

    @Override
    public StoredFile upload(MultipartFile file, String directory) {
        try {
            return upload(
                file.getBytes(),
                file.getContentType(),
                file.getOriginalFilename(),
                directory
            );
        } catch (IOException exception) {
            log.error("파일 읽기 실패: filename={}", file.getOriginalFilename(), exception);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "업로드할 파일을 읽지 못했습니다.");
        }
    }

    @Override
    public StoredFile upload(byte[] content, String contentType, String originalFilename, String directory) {
        String path = buildObjectPath(directory, originalFilename);
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, path)
            .setContentType(contentType)
            .build();

        try {
            storage.create(blobInfo, content);
        } catch (RuntimeException exception) {
            log.error("GCS 업로드 실패: path={}", path, exception);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return new StoredFile(path, bucketName, getPublicUrl(path));
    }

    @Override
    public void delete(String path) {
        try {
            storage.delete(bucketName, path);
        } catch (RuntimeException exception) {
            log.warn("GCS 파일 삭제 실패: path={}", path, exception);
        }
    }

    @Override
    public String getPublicUrl(String path) {
        return GCS_URL_PREFIX + bucketName + "/" + path;
    }

    @Override
    public String generateDownloadUrl(String path, Duration duration) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, path).build();
        URL signedUrl = storage.signUrl(
            blobInfo,
            duration.toMinutes(),
            TimeUnit.MINUTES,
            SignUrlOption.withV4Signature(),
            SignUrlOption.httpMethod(HttpMethod.GET)
        );
        return signedUrl.toString();
    }

    private String buildObjectPath(String directory, String originalFilename) {
        String normalizedDirectory = directory.endsWith("/")
            ? directory.substring(0, directory.length() - 1)
            : directory;
        return normalizedDirectory + "/" + UUID.randomUUID() + "-" + sanitizeFilename(originalFilename);
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }

        String normalized = new String(
            originalFilename.trim().getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8
        );
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
