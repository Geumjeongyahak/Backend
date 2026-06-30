package geumjeongyahak.domain.file.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.common.validation.FileValidationSupport;
import geumjeongyahak.domain.channel.service.ChannelAccessChecker;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;

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
    private final PostAttachmentRepository postAttachmentRepository;
    private final ChannelAccessChecker channelAccessChecker;

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

    public String getDownloadUrl(UUID fileId, CustomUserDetails userDetails) {
        File file = getFile(fileId);
        if (!canDownload(fileId, userDetails)) {
            throw new AccessDeniedException("첨부파일 다운로드 권한이 없습니다.");
        }
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

    private boolean canDownload(UUID fileId, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isAdmin()) {
            return true;
        }
        return postAttachmentRepository.findPublishedPostChannelIdsByFileId(fileId).stream()
            .anyMatch(channelId -> channelAccessChecker.can("read", channelId, userDetails));
    }
}
