package geumjeongyahak.domain.sitecontent.service.access;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.service.access.AttachmentReadPolicy;
import geumjeongyahak.domain.sitecontent.repository.SiteHistoryRepository;
import lombok.RequiredArgsConstructor;

/** 연혁 사진: 공개 사이트에 나가므로 비로그인 포함 누구나. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteHistoryPhotoReadPolicy implements AttachmentReadPolicy {

    private final SiteHistoryRepository siteHistoryRepository;

    @Override
    public boolean canRead(UUID fileId, CustomUserDetails user) {
        return siteHistoryRepository.existsPhotoByFileId(fileId);
    }
}
