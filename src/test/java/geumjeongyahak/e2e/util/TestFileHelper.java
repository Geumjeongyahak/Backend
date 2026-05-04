package geumjeongyahak.e2e.util;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.specification.MultiPartSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;

@Service
@Transactional
public class TestFileHelper {
    private final FileRepository fileRepository;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;

    public TestFileHelper(
            FileRepository fileRepository,
            PostFileRepository postFileRepository,
            PostAttachmentRepository postAttachmentRepository) {
        this.fileRepository = fileRepository;
        this.postFileRepository = postFileRepository;
        this.postAttachmentRepository = postAttachmentRepository;
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

    public void clearAll() {
        postAttachmentRepository.deleteAll();
        postFileRepository.deleteAll();
        fileRepository.deleteAll();
        postAttachmentRepository.flush();
        postFileRepository.flush();
        fileRepository.flush();
    }
}
