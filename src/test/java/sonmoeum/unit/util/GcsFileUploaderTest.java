package sonmoeum.unit.util;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;
import sonmoeum.common.util.GcsFileUploader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GcsFileUploaderTest {

    private static final String BUCKET_NAME = "test-bucket";

    @Mock
    private Storage storage;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private GcsFileUploader gcsFileUploader;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gcsFileUploader, "bucketName", BUCKET_NAME);
    }

    @Test
    @DisplayName("upload - 파일 업로드 성공 시 GCS 공개 URL을 반환한다")
    void upload_success() throws IOException {
        // given
        String directory = "profiles";
        String originalFilename = "avatar.png";
        String contentType = "image/png";
        byte[] content = "image-bytes".getBytes();

        given(file.getOriginalFilename()).willReturn(originalFilename);
        given(file.getContentType()).willReturn(contentType);
        given(file.getBytes()).willReturn(content);

        // when
        String result = gcsFileUploader.upload(file, directory);

        // then
        assertThat(result).startsWith("https://storage.googleapis.com/" + BUCKET_NAME + "/" + directory + "/");
        assertThat(result).endsWith("_" + originalFilename);

        ArgumentCaptor<BlobInfo> blobCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).create(blobCaptor.capture(), eq(content));
        BlobInfo captured = blobCaptor.getValue();
        assertThat(captured.getBucket()).isEqualTo(BUCKET_NAME);
        assertThat(captured.getName()).startsWith(directory + "/");
        assertThat(captured.getContentType()).isEqualTo(contentType);
    }

    @Test
    @DisplayName("upload - 파일 읽기 중 IOException 발생 시 FILE_UPLOAD_FAILED 예외를 던진다")
    void upload_ioException_throwsBusinessException() throws IOException {
        // given
        given(file.getOriginalFilename()).willReturn("fail.jpg");
        given(file.getContentType()).willReturn("image/jpeg");
        given(file.getBytes()).willThrow(new IOException("disk error"));

        // when
        BusinessException ex = catchThrowableOfType(
                BusinessException.class,
                () -> gcsFileUploader.upload(file, "docs")
        );

        // then
        assertThat(ex.getCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED.getCode());
        assertThat(ex.getStatus()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED.getStatus());
    }

    @Test
    @DisplayName("upload - 생성된 파일명에 UUID가 포함되어 중복되지 않는다")
    void upload_generatesUniqueFileName() throws IOException {
        // given
        given(file.getOriginalFilename()).willReturn("photo.jpg");
        given(file.getContentType()).willReturn("image/jpeg");
        given(file.getBytes()).willReturn(new byte[0]);

        // when
        String url1 = gcsFileUploader.upload(file, "images");
        String url2 = gcsFileUploader.upload(file, "images");

        // then
        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("delete - 파일 URL에서 버킷 경로를 제거하여 올바른 blob 이름으로 삭제한다")
    void delete_success() {
        // given
        String blobName = "profiles/some-uuid_avatar.png";
        String fileUrl = "https://storage.googleapis.com/" + BUCKET_NAME + "/" + blobName;

        // when
        gcsFileUploader.deleteByUrl(fileUrl);

        // then
        verify(storage).delete(BUCKET_NAME, blobName);
    }

    @Test
    @DisplayName("delete - 중첩 디렉토리 경로도 올바르게 추출하여 삭제한다")
    void delete_nestedDirectory() {
        // given
        String blobName = "dept/2024/uuid_report.pdf";
        String fileUrl = "https://storage.googleapis.com/" + BUCKET_NAME + "/" + blobName;

        // when
        gcsFileUploader.deleteByUrl(fileUrl);

        // then
        verify(storage).delete(BUCKET_NAME, blobName);
    }
}