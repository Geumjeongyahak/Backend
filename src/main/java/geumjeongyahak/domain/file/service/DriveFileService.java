package geumjeongyahak.domain.file.service;

import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.v1.dto.request.RegisterDriveFileRequest;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriveFileService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String DEFAULT_EXTENSION = "drive";
    private static final Pattern DRIVE_FILE_PATH_PATTERN = Pattern.compile("/(?:file/d|document/d|spreadsheets/d|presentation/d|folders)/([^/?#]+)");

    private final FileRepository fileRepository;

    @Transactional
    public FileUploadResponse registerDriveFile(RegisterDriveFileRequest request) {
        String driveUrl = request.driveUrl().trim();
        String originalName = request.originalName().trim();
        String contentType = normalizeContentType(request.mimeType());
        String storageKey = extractDriveFileId(driveUrl).orElse(driveUrl);
        String ext = resolveExtension(originalName);

        File file = fileRepository.findByPublicUrlAndIsGoogleDriveTrue(driveUrl)
                .map(existingFile -> {
                    existingFile.updateDriveMetadata(storageKey, originalName, contentType, request.fileSize(), ext, driveUrl);
                    return existingFile;
                })
                .orElseGet(() -> fileRepository.save(File.builder()
                        .storageKey(storageKey)
                        .bucket(File.GOOGLE_DRIVE_BUCKET)
                        .originalName(originalName)
                        .contentType(contentType)
                        .fileSize(request.fileSize())
                        .ext(ext)
                        .publicUrl(driveUrl)
                        .isGoogleDrive(true)
                        .build()));

        log.info("Google Drive 파일 메타데이터 등록 완료 - fileId: {}, storageKey: {}", file.getId(), storageKey);
        return FileUploadResponse.from(file, driveUrl);
    }

    private String normalizeContentType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String originalName) {
        if (originalName == null || originalName.isBlank() || !originalName.contains(".")) {
            return DEFAULT_EXTENSION;
        }

        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).trim();
        if (extension.isBlank()) {
            return DEFAULT_EXTENSION;
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private Optional<String> extractDriveFileId(String driveUrl) {
        try {
            URI uri = URI.create(driveUrl);
            Matcher pathMatcher = DRIVE_FILE_PATH_PATTERN.matcher(uri.getPath());
            if (pathMatcher.find()) {
                return Optional.of(URLDecoder.decode(pathMatcher.group(1), StandardCharsets.UTF_8));
            }

            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return Optional.empty();
            }

            for (String parameter : query.split("&")) {
                int separatorIndex = parameter.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String name = URLDecoder.decode(parameter.substring(0, separatorIndex), StandardCharsets.UTF_8);
                if ("id".equals(name)) {
                    return Optional.of(URLDecoder.decode(parameter.substring(separatorIndex + 1), StandardCharsets.UTF_8));
                }
            }
        } catch (IllegalArgumentException exception) {
            log.debug("Google Drive 파일 ID 추출 실패 - driveUrl: {}", driveUrl, exception);
        }

        return Optional.empty();
    }
}
