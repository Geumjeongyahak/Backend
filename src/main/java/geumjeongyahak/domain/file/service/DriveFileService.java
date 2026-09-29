package geumjeongyahak.domain.file.service;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.config.AppConfig;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
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
    // files 테이블 컬럼 길이. Drive 가 준 값이 넘으면 DB 오류(500) 대신 400 으로 거절한다
    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;
    private static final int MAX_EXTENSION_LENGTH = 20;
    private static final String SCOPE_CLASSROOM = "classroom";
    private static final String SCOPE_DEPARTMENT = "department";
    private static final Pattern DRIVE_FILE_PATH_PATTERN = Pattern.compile("/(?:file/d|document/d|spreadsheets/d|presentation/d|folders)/([^/?#]+)");

    private final FileRepository fileRepository;
    private final DriveStorageService driveStorageService;
    private final FileValidationSupport fileValidationSupport;
    private final ClassroomProxyService classroomProxyService;
    private final DepartmentProxyService departmentProxyService;

    // 이름 · 형식 · 크기는 요청 값이 아니라 Drive 조회 결과로 저장한다. 서버가 읽을 수 없는 파일은 거절한다.
    // Drive 조회(토큰 갱신 포함)가 DB 연결을 잡지 않도록 트랜잭션 밖에서 돌고, 조회 · 저장은 저장소 호출마다 짧게 끝난다
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FileUploadResponse registerDriveFile(RegisterDriveFileRequest request) {
        String driveUrl = request.driveUrl().trim();
        String storageKey = extractDriveFileId(driveUrl)
            .orElseThrow(() -> new BadRequestException(CommonErrorCode.INVALID_INPUT, "Google Drive 파일 URL만 등록할 수 있습니다."));

        Optional<File> existing = fileRepository.findByPublicUrlAndIsGoogleDriveTrue(driveUrl);
        if (existing.isPresent() && !existing.get().isDeleted()) {
            return FileUploadResponse.from(existing.get(), driveUrl);
        }

        DriveStorageService.StoredDriveFile driveFile = driveStorageService.getMetadata(storageKey);
        String originalName = driveFile.name();
        String contentType = normalizeContentType(driveFile.mimeType());
        if (originalName.length() > MAX_ORIGINAL_NAME_LENGTH || contentType.length() > MAX_CONTENT_TYPE_LENGTH) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "Google Drive 파일 정보가 너무 깁니다. 파일명은 255자 이하여야 합니다.");
        }
        String ext = resolveExtension(originalName);

        // 지웠던 링크를 다시 등록하면 Drive 값으로 되살린다
        File file = existing
                .map(deletedFile -> {
                    deletedFile.updateDriveMetadata(storageKey, originalName, contentType, driveFile.size(), ext, driveUrl);
                    return fileRepository.save(deletedFile);
                })
                .orElseGet(() -> fileRepository.save(File.builder()
                        .storageKey(storageKey)
                        .bucket(File.GOOGLE_DRIVE_BUCKET)
                        .originalName(originalName)
                        .contentType(contentType)
                        .fileSize(driveFile.size())
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

        YearMonth now = YearMonth.now(AppConfig.ZONE_ID);
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
        if (extension.isBlank() || extension.length() > MAX_EXTENSION_LENGTH) {
            return DEFAULT_EXTENSION;
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private Optional<String> extractDriveFileId(String driveUrl) {
        try {
            URI uri = URI.create(driveUrl);
            String host = uri.getHost();
            if (host == null || !(host.equals("drive.google.com") || host.endsWith(".drive.google.com")
                || host.equals("docs.google.com") || host.endsWith(".docs.google.com"))) {
                return Optional.empty();
            }
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
