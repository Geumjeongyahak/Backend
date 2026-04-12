package sonmoeum.common.validation;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import sonmoeum.common.config.FileProperties;
import sonmoeum.common.exception.BadRequestException;
import sonmoeum.common.exception.CommonErrorCode;

@Slf4j
@Component
public class FileValidationSupport {

    private final FileProperties fileProperties;

    public FileValidationSupport(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    public void validateImage(MultipartFile file) {
        validateFilePresence(file);
        validateSize(
            file,
            fileProperties.getImage().getMaxSize().toBytes(),
            "이미지 파일은 최대 " + fileProperties.getImage().getMaxSize().toMegabytes() + "MB까지 업로드할 수 있습니다."
        );
        validateMimeType(file, normalizeMimeTypes(fileProperties.getImage().getAllowedMimeTypes()), "지원하지 않는 이미지 형식입니다.");
    }

    public void validateDocument(MultipartFile file) {
        validateFilePresence(file);
        validateSize(
            file,
            fileProperties.getDocument().getMaxSize().toBytes(),
            "문서 파일은 최대 " + fileProperties.getDocument().getMaxSize().toMegabytes() + "MB까지 업로드할 수 있습니다."
        );
        validateMimeType(file, normalizeMimeTypes(fileProperties.getDocument().getAllowedMimeTypes()), "지원하지 않는 문서 형식입니다.");
    }

    public String extractExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "파일 확장자를 확인할 수 없습니다.");
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void validateFilePresence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(CommonErrorCode.MISSING_REQUIRED_FIELD, "업로드할 파일이 비어 있습니다.");
        }
    }

    private void validateSize(MultipartFile file, long maxBytes, String message) {
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, message);
        }
    }

    private void validateMimeType(MultipartFile file, Set<String> allowedTypes, String message) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            log.warn("허용되지 않은 MIME 타입: {}", contentType);
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, message);
        }
    }

    private Set<String> normalizeMimeTypes(Iterable<String> mimeTypes) {
        return mimeTypes == null
            ? Set.of()
            : StreamSupport.stream(mimeTypes.spliterator(), false)
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
