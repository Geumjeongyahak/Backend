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

    /**
     * 사이트 콘텐츠 이미지 업로드(`/images/site-contents`)로 올라간 살아 있는 파일을 공개 URL로 찾는다.
     * 게시글 등 다른 곳에 올린 파일은 찾지 않는다 — 연결되면 연혁에서 사진을 뺄 때 남의 파일이 지워진다.
     */
    @Transactional(readOnly = true)
    public Optional<File> findActiveSiteContentImageByPublicUrl(String publicUrl) {
        return fileRepository.findFirstByPublicUrlAndStorageKeyStartingWithAndIsDeletedFalse(
            publicUrl, ImageUploadService.SITE_CONTENT_DIRECTORY + "/"
        );
    }
}
