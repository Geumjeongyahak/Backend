package geumjeongyahak.e2e;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Storage;

import geumjeongyahak.domain.file.service.StorageService;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;
import geumjeongyahak.domain.file.service.DriveStorageService;
import geumjeongyahak.common.mail.MailDeliveryResult;
import geumjeongyahak.common.mail.MailSenderService;

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

    @Bean
    @Primary
    DriveStorageService testDriveStorageService() {
        return new ControlledDriveStorageService();
    }

    @Bean
    @Primary
    ControlledMailSenderService testMailSenderService() {
        return new ControlledMailSenderService();
    }

    public static class ControlledMailSenderService implements MailSenderService {
        private String lastSignupRecipient;
        private String lastPasswordResetRecipient;
        private String lastPasswordResetCode;
        private String lastEmailVerificationRecipient;
        private String lastEmailVerificationCode;
        private String lastEmailVerificationUrl;

        @Override
        public MailDeliveryResult sendSignupWelcomeMail(String recipientEmail, String recipientName) {
            this.lastSignupRecipient = recipientEmail;
            return MailDeliveryResult.sent("signup-welcome", recipientEmail);
        }

        @Override
        public MailDeliveryResult sendPasswordResetMail(String recipientEmail, String recipientName, String resetCode) {
            this.lastPasswordResetRecipient = recipientEmail;
            this.lastPasswordResetCode = resetCode;
            return MailDeliveryResult.sent("password-reset", recipientEmail);
        }

        @Override
        public MailDeliveryResult sendEmailVerificationMail(
            String recipientEmail,
            String recipientName,
            String verificationCode
        ) {
            this.lastEmailVerificationRecipient = recipientEmail;
            this.lastEmailVerificationCode = verificationCode;
            this.lastEmailVerificationUrl = "/api/v1/auth/email-verification/confirm?token=" + verificationCode;
            return MailDeliveryResult.sent("email-verification", recipientEmail);
        }

        public String getLastSignupRecipient() {
            return lastSignupRecipient;
        }

        public String getLastPasswordResetRecipient() {
            return lastPasswordResetRecipient;
        }

        public String getLastPasswordResetCode() {
            return lastPasswordResetCode;
        }

        public String getLastEmailVerificationRecipient() {
            return lastEmailVerificationRecipient;
        }

        public String getLastEmailVerificationCode() {
            return lastEmailVerificationCode;
        }

        public String getLastEmailVerificationUrl() {
            return lastEmailVerificationUrl;
        }
    }

    public static class ControlledDriveStorageService implements DriveStorageService {
        private final Map<String, byte[]> files = new HashMap<>();

        @Override
        public StoredDriveFile upload(DriveUploadTarget target, List<String> folderPath, MultipartFile file) {
            String driveFileId = target.path() + "-drive-file";
            String viewUrl = "https://drive.google.com/file/d/" + driveFileId + "/view";
            String downloadUrl = "https://drive.google.com/uc?export=download&id=" + driveFileId;
            files.put(driveFileId, bytes(file));
            return new StoredDriveFile(
                driveFileId,
                viewUrl,
                downloadUrl,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
            );
        }

        @Override
        public byte[] download(String fileId) {
            return files.getOrDefault(fileId, new byte[0]);
        }
    }

    public static class ControlledStorageService implements StorageService {
        private static final String TEST_BUCKET = "test-bucket";
        private final Set<String> failDeletePaths = new HashSet<>();
        private final Set<String> deletedPaths = new HashSet<>();
        private final Set<String> uploadedPaths = new HashSet<>();
        private final Map<String, byte[]> files = new HashMap<>();

        public void failDeleteFor(String path) {
            failDeletePaths.add(path);
        }

        public void resetFailPaths() {
            failDeletePaths.clear();
            deletedPaths.clear();
            uploadedPaths.clear();
        }

        public Set<String> getDeletedPaths() {
            return Set.copyOf(deletedPaths);
        }

        public Set<String> getUploadedPaths() {
            return Set.copyOf(uploadedPaths);
        }

        @Override
        public StoredFile upload(MultipartFile file, String directory) {
            return upload(
                bytes(file),
                file.getContentType(),
                file.getOriginalFilename(),
                directory
            );
        }

        @Override
        public StoredFile upload(byte[] content, String contentType, String originalFilename, String directory) {
            String safeName = originalFilename == null ? "file" : originalFilename.replace(" ", "_");
            String path = directory + "/" + UUID.randomUUID() + "-" + safeName;
            uploadedPaths.add(path);
            files.put(path, content);
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
        public byte[] download(String path) {
            return files.getOrDefault(path, new byte[0]);
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

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("테스트 파일을 읽을 수 없습니다.", exception);
        }
    }
}
