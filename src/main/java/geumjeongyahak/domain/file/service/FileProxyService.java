package geumjeongyahak.domain.file.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;

@Service
@RequiredArgsConstructor
public class FileProxyService {

    private final FileRepository fileRepository;

    @Transactional(readOnly = true)
    public File getReferenceById(UUID fileId) {
        return fileRepository.getReferenceById(fileId);
    }

    @Transactional(readOnly = true)
    public File getActiveById(UUID fileId) {
        return fileRepository.findByIdAndIsDeletedFalse(fileId)
            .orElseThrow(() -> new ResourceNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND, "파일을 찾을 수 없습니다."));
    }

    /** 업로드 응답의 공개 URL로 살아 있는 파일을 찾는다. 외부 URL이면 비어 있다. */
    @Transactional(readOnly = true)
    public Optional<File> findActiveByPublicUrl(String publicUrl) {
        return fileRepository.findFirstByPublicUrlAndIsDeletedFalse(publicUrl);
    }
}
