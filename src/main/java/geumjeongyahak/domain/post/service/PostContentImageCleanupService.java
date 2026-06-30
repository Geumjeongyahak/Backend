package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.event.EventPublisher;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.post.entity.PostFile;
import geumjeongyahak.domain.post.event.PostImageDeleteRequestedEvent;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostContentImageCleanupService {

    private static final Pattern IMG_SRC_PATTERN = Pattern.compile(
            "<img\\b[^>]*\\bsrc\\s*=\\s*([\"'])(.*?)\\1",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final PostFileRepository postFileRepository;
    private final EventPublisher eventPublisher;

    public void deleteUnusedImages(Long postId, String contentHtml) {
        Set<UUID> unusedFileIds = findUnusedImageFileIds(postId, contentHtml);
        if (unusedFileIds.isEmpty()) {
            return;
        }

        eventPublisher.publish(new PostImageDeleteRequestedEvent(postId, unusedFileIds));
        log.info("게시글 미사용 이미지 삭제 이벤트 발행 - postId: {}, count: {}", postId, unusedFileIds.size());
    }

    public Optional<String> findFirstUsedImageUrl(Long postId, String contentHtml) {
        Set<String> usedImageUrls = extractImageUrls(contentHtml);
        if (usedImageUrls.isEmpty()) {
            return Optional.empty();
        }

        return findLinkedImages(postId).stream()
                .map(PostFile::getFile)
                .filter(file -> !file.isDeleted())
                .map(File::getPublicUrl)
                .filter(StringUtils::hasText)
                .filter(usedImageUrls::contains)
                .findFirst();
    }

    private Set<UUID> findUnusedImageFileIds(Long postId, String contentHtml) {
        Set<String> usedImageUrls = extractImageUrls(contentHtml);
        List<PostFile> linkedImages = findLinkedImages(postId);

        Set<UUID> unusedFileIds = new LinkedHashSet<>();
        for (PostFile linkedImage : linkedImages) {
            File file = linkedImage.getFile();
            if (file.isDeleted()) {
                continue;
            }
            if (!StringUtils.hasText(file.getPublicUrl()) || !usedImageUrls.contains(file.getPublicUrl())) {
                unusedFileIds.add(file.getId());
            }
        }
        if (unusedFileIds.isEmpty()) {
            return unusedFileIds;
        }

        Set<UUID> referencedFileIds = new LinkedHashSet<>(
                postFileRepository.findReferencedFileIdsByFileIdInAndPostIdNot(unusedFileIds, postId)
        );
        unusedFileIds.removeAll(referencedFileIds);
        return unusedFileIds;
    }

    private List<PostFile> findLinkedImages(Long postId) {
        return postFileRepository.findAllByPostIdWithFileOrderBySortOrderAsc(postId);
    }

    private Set<String> extractImageUrls(String contentHtml) {
        if (!StringUtils.hasText(contentHtml)) {
            return Set.of();
        }

        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = IMG_SRC_PATTERN.matcher(contentHtml);
        while (matcher.find()) {
            String url = HtmlUtils.htmlUnescape(matcher.group(2)).trim();
            if (StringUtils.hasText(url)) {
                urls.add(url);
            }
        }
        return urls;
    }
}
