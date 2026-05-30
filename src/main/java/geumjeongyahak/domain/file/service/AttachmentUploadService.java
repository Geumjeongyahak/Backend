package geumjeongyahak.domain.file.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.validation.FileValidationSupport;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentUploadService {

    private static final String ATTACHMENT_DIRECTORY = "documents/attachments";
    private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(30);

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final FileValidationSupport fileValidationSupport;

    @Transactional
    public FileUploadResponse uploadAttachment(MultipartFile file) {
        fileValidationSupport.validateDocument(file);

        StorageService.StoredFile storedFile = storageService.upload(file, ATTACHMENT_DIRECTORY);
        File savedFile = fileRepository.save(
            File.builder()
                .storageKey(storedFile.path())
                .bucket(storedFile.bucket())
                .originalName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .ext(fileValidationSupport.extractExtension(file.getOriginalFilename()))
                .publicUrl(storedFile.url())
                .build()
        );

        log.debug("첨부파일 업로드 완료 (fileId={})", savedFile.getId());
        return FileUploadResponse.from(savedFile, storedFile.url());
    }

    public String getDownloadUrl(UUID fileId) {
        File file = getFile(fileId);
        if (file.isGoogleDriveFile()) {
            return file.getPublicUrl();
        }
        return storageService.generateDownloadUrl(file.getStorageKey(), DOWNLOAD_URL_DURATION);
    }

    @Transactional
    public void deleteAttachment(UUID fileId) {
        File file = getFile(fileId);
        if (!file.isGoogleDriveFile()) {
            storageService.delete(file.getStorageKey());
        }
        file.delete();
    }

    @Transactional
    public void deleteAttachmentIfPresent(UUID fileId) {
        fileRepository.findById(fileId)
            .filter(file -> !file.isDeleted())
            .ifPresent(file -> {
                if (!file.isGoogleDriveFile()) {
                    storageService.delete(file.getStorageKey());
                }
                file.delete();
            });
    }

    private File getFile(UUID fileId) {
        return fileRepository.findByIdAndIsDeletedFalse(fileId)
            .orElseThrow(() -> new ResourceNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND, "파일을 찾을 수 없습니다."));
    }
}
