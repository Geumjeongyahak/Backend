package geumjeongyahak.e2e.util;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.specification.MultiPartSpecification;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.post.entity.PostAttachment;
import geumjeongyahak.domain.post.entity.PostFile;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.post.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class TestFileHelper {
    private final FileRepository fileRepository;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostRepository postRepository;

    public TestFileHelper(
            FileRepository fileRepository,
            PostFileRepository postFileRepository,
            PostAttachmentRepository postAttachmentRepository,
            PostRepository postRepository) {
        this.fileRepository = fileRepository;
        this.postFileRepository = postFileRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.postRepository = postRepository;
    }

    public MultiPartSpecification multipartImageRequest(String fieldName, String filename) {
        return new MultiPartSpecBuilder("test-image-content".getBytes())
                .controlName(fieldName)
                .fileName(filename)
                .mimeType("image/png")
                .build();
    }

    public MultiPartSpecification multipartAttachmentRequest(String fieldName, String filename) {
        return new MultiPartSpecBuilder("test-attachment-content".getBytes())
                .controlName(fieldName)
                .fileName(filename)
                .mimeType("application/pdf")
                .build();
    }

    public UUID createOrphanedFile(LocalDateTime deletedAt) {
        return createOrphanedFileWithKey(deletedAt, "test/orphan/" + UUID.randomUUID() + ".pdf");
    }

    public UUID createOrphanedFileWithKey(LocalDateTime deletedAt, String storageKey) {
        File file = File.builder()
                .storageKey(storageKey)
                .bucket("test-bucket")
                .originalName("orphan.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .ext("pdf")
                .publicUrl("https://test-storage.local/test-bucket/" + storageKey)
                .build();
        file.delete();
        fileRepository.save(file);
        ReflectionTestUtils.setField(file, "deletedAt", deletedAt);
        return fileRepository.save(file).getId();
    }

    public UUID createDeletedDriveFile(LocalDateTime deletedAt, String driveUrl) {
        String storageKey = "drive-file-" + UUID.randomUUID();
        File file = File.builder()
                .storageKey(storageKey)
                .bucket(File.GOOGLE_DRIVE_BUCKET)
                .originalName("drive.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .ext("pdf")
                .publicUrl(driveUrl)
                .isGoogleDrive(true)
                .build();
        file.delete();
        fileRepository.save(file);
        ReflectionTestUtils.setField(file, "deletedAt", deletedAt);
        return fileRepository.save(file).getId();
    }

    public void linkFileToPost(Long postId, UUID fileId) {
        postFileRepository.save(PostFile.builder()
                .post(postRepository.getReferenceById(postId))
                .file(fileRepository.getReferenceById(fileId))
                .build());
    }

    public void linkAttachmentToPost(Long postId, UUID fileId) {
        postAttachmentRepository.save(PostAttachment.builder()
                .post(postRepository.getReferenceById(postId))
                .file(fileRepository.getReferenceById(fileId))
                .build());
    }

    public void clearAll() {
        postAttachmentRepository.deleteAll();
        postFileRepository.deleteAll();
        fileRepository.deleteAll();
        postAttachmentRepository.flush();
        postFileRepository.flush();
        fileRepository.flush();
    }
}
