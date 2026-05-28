package geumjeongyahak.e2e;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Storage;

import geumjeongyahak.domain.file.service.StorageService;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestStorageConfig {

    @Bean
    @Primary
    Storage googleCloudStorage() {
        return mock(Storage.class);
    }

    @Bean
    @Primary
    ControlledStorageService testStorageService() {
        return new ControlledStorageService();
    }

    public static class ControlledStorageService implements StorageService {
        private static final String TEST_BUCKET = "test-bucket";
        private final Set<String> failDeletePaths = new HashSet<>();
        private final Set<String> deletedPaths = new HashSet<>();

        public void failDeleteFor(String path) {
            failDeletePaths.add(path);
        }

        public void resetFailPaths() {
            failDeletePaths.clear();
            deletedPaths.clear();
        }

        public Set<String> getDeletedPaths() {
            return Set.copyOf(deletedPaths);
        }

        @Override
        public StoredFile upload(MultipartFile file, String directory) {
            return upload(
                new byte[0],
                file.getContentType(),
                file.getOriginalFilename(),
                directory
            );
        }

        @Override
        public StoredFile upload(byte[] content, String contentType, String originalFilename, String directory) {
            String safeName = originalFilename == null ? "file" : originalFilename.replace(" ", "_");
            String path = directory + "/" + UUID.randomUUID() + "-" + safeName;
            return new StoredFile(path, TEST_BUCKET, getPublicUrl(path));
        }

        @Override
        public boolean delete(String path) {
            boolean deleted = !failDeletePaths.contains(path);
            if (deleted) {
                deletedPaths.add(path);
            }
            return deleted;
        }

        @Override
        public String getPublicUrl(String path) {
            return "https://test-storage.local/" + TEST_BUCKET + "/" + path;
        }

        @Override
        public String generateDownloadUrl(String path, Duration duration) {
            return "https://test-storage.local/" + TEST_BUCKET + "/" + path + "?expires=" + duration.toMinutes();
        }
    }
}
