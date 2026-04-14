package geumjeongyahak.common.util;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {
    String upload(MultipartFile file, String directory);
    void deleteByUrl(String fileUrl);
}
