package sonmoeum.e2e;

import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Storage;

import sonmoeum.domain.file.service.StorageService;

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
    StorageService testStorageService() {
        return new StorageService() {
            private static final String TEST_BUCKET = "test-bucket";

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
            public void delete(String path) {
            }

            @Override
            public String getPublicUrl(String path) {
                return "https://test-storage.local/" + TEST_BUCKET + "/" + path;
            }

            @Override
            public String generateDownloadUrl(String path, Duration duration) {
                return "https://test-storage.local/" + TEST_BUCKET + "/" + path + "?expires=" + duration.toMinutes();
            }
        };
    }
}
