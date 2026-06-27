package geumjeongyahak.domain.file.service;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.validation.FileValidationSupport;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.v1.dto.request.RegisterDriveFileRequest;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
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
    private static final String SCOPE_CLASSROOM = "classroom";
    private static final String SCOPE_DEPARTMENT = "department";
    private static final ZoneId DRIVE_FOLDER_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern DRIVE_FILE_PATH_PATTERN = Pattern.compile("/(?:file/d|document/d|spreadsheets/d|presentation/d|folders)/([^/?#]+)");

    private final FileRepository fileRepository;
    private final DriveStorageService driveStorageService;
    private final FileValidationSupport fileValidationSupport;
    private final ClassroomProxyService classroomProxyService;
    private final DepartmentProxyService departmentProxyService;

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

    @Transactional
    public FileUploadResponse uploadDriveFile(DriveUploadTarget target, MultipartFile multipartFile) {
        return uploadDriveFile(target, null, null, multipartFile);
    }

    @Transactional
    public FileUploadResponse uploadDriveFile(
        DriveUploadTarget target,
        String scopeType,
        Long scopeId,
        MultipartFile multipartFile
    ) {
        fileValidationSupport.validateDocument(multipartFile);

        DriveStorageService.StoredDriveFile uploaded = driveStorageService.upload(
            target,
            resolveFolderPath(target, scopeType, scopeId),
            multipartFile
        );
        String originalName = StringUtils.hasText(uploaded.name()) ? uploaded.name() : multipartFile.getOriginalFilename();
        String contentType = normalizeContentType(uploaded.mimeType());
        Long fileSize = uploaded.size() != null ? uploaded.size() : multipartFile.getSize();
        String viewUrl = uploaded.viewUrl();

        File savedFile = fileRepository.save(File.builder()
            .storageKey(uploaded.fileId())
            .bucket(File.GOOGLE_DRIVE_BUCKET)
            .originalName(originalName)
            .contentType(contentType)
            .fileSize(fileSize)
            .ext(resolveExtension(originalName))
            .publicUrl(viewUrl)
            .isGoogleDrive(true)
            .build());

        log.info("Google Drive 파일 업로드 완료 - fileId: {}, storageKey: {}", savedFile.getId(), uploaded.fileId());
        return FileUploadResponse.from(savedFile, viewUrl);
    }

    List<String> resolveFolderPath(DriveUploadTarget target, String scopeType, Long scopeId) {
        boolean hasScopeType = StringUtils.hasText(scopeType);
        boolean hasScopeId = scopeId != null;
        if (hasScopeType != hasScopeId) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "scopeType과 scopeId는 함께 전달해야 합니다.");
        }

        YearMonth now = YearMonth.now(DRIVE_FOLDER_ZONE);
        String year = Integer.toString(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        if (target != DriveUploadTarget.BOARD) {
            if (hasScopeType) {
                throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "이 Drive 업로드 대상은 scope를 지원하지 않습니다.");
            }
            return List.of(year, month);
        }

        if (!hasScopeType) {
            return List.of("공통", year, month);
        }

        String normalizedScopeType = scopeType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedScopeType) {
            case SCOPE_CLASSROOM -> {
                Classroom classroom = classroomProxyService.getActiveById(scopeId);
                yield List.of("반별", classroom.getName(), year, month);
            }
            case SCOPE_DEPARTMENT -> {
                Department department = departmentProxyService.getById(scopeId);
                yield List.of("부서별", department.getName(), year, month);
            }
            default -> throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "지원하지 않는 Drive scope입니다.");
        };
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
