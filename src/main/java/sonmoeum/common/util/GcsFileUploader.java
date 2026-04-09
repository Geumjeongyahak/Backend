package sonmoeum.common.util;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import sonmoeum.common.exception.BusinessException;
import sonmoeum.common.exception.ErrorCode;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GcsFileUploader implements FileUploader {

    private static final String GCS_URL_PREFIX = "https://storage.googleapis.com/";

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    @Override
    public String upload(MultipartFile file, String directory) {
        String fileName = directory + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName)
                .setContentType(file.getContentType())
                .build();
        try {
            storage.create(blobInfo, file.getBytes());
        } catch (IOException e) {
            log.error("파일 업로드 실패: directory={}, filename={}, error={}", directory, file.getOriginalFilename(), e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        String fileUrl = GCS_URL_PREFIX + bucketName + "/" + fileName;
        log.info("파일 업로드 성공: url={}", fileUrl);
        return fileUrl;
    }

    public void deleteByUrl(String fileUrl) {
        String fileName = fileUrl.replace(GCS_URL_PREFIX + bucketName + "/", "");
        storage.delete(bucketName, fileName);
        log.info("파일 삭제 성공: url={}", fileUrl);
    }
}
