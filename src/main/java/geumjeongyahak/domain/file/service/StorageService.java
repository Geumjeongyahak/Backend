package geumjeongyahak.domain.file.service;

import java.time.Duration;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFile upload(MultipartFile file, String directory);

    StoredFile upload(byte[] content, String contentType, String originalFilename, String directory);

    boolean delete(String path);

    String getPublicUrl(String path);

    String generateDownloadUrl(String path, Duration duration);

    record StoredFile(String path, String bucket, String url) {
    }
}
