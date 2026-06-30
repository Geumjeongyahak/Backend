package geumjeongyahak.domain.file.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import geumjeongyahak.domain.file.enums.DriveUploadTarget;

public interface DriveStorageService {

    default StoredDriveFile upload(DriveUploadTarget target, MultipartFile file) {
        return upload(target, List.of(), file);
    }

    StoredDriveFile upload(DriveUploadTarget target, List<String> folderPath, MultipartFile file);

    record StoredDriveFile(
        String fileId,
        String viewUrl,
        String downloadUrl,
        String name,
        String mimeType,
        Long size
    ) {
    }
}
