package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.security.service.CustomUserDetails;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.enums.PostType;
import geumjeongyahak.domain.post.repository.PostSpecs;
import geumjeongyahak.domain.post.v1.dto.request.PostSearchRequest;

/**
 * 게시글 검색 조건을 Specification 으로 변환한다.
 */
@Component
public class PostSearchSpecificationBuilder {

    public Specification<Post> build(PostSearchRequest request, CustomUserDetails userDetails) {
        // 1. 기본 필터: 삭제되지 않은 게시글 + 삭제되지 않은 채널
        Specification<Post> spec = PostSpecs.withoutDeleted()
                .and((root, query, cb) -> cb.isFalse(root.get("channel").get("isDeleted")));

        // 2. 가시성 필터 (ADMIN 및 권한 보유자 대응)
        if (userDetails == null) {
            // 미인증: 공개 채널만 노출
            spec = spec.and(PostSpecs.hasPublicAccess());
        } else if (!userDetails.isAdmin()) {
            // ADMIN이 아닌 경우
            if (!hasWildcardPostPermission(userDetails)) {
                // 전체 조회 권한(*)이 없는 경우: 공개 채널 + 개별 권한 있는 채널만 노출
                java.util.List<Long> allowedIds = extractAllowedChannelIds(userDetails);
                Specification<Post> visibility = PostSpecs.hasPublicAccess();
                if (!allowedIds.isEmpty()) {
                    visibility = visibility.or(PostSpecs.hasAnyChannelId(allowedIds));
                }
                spec = spec.and(visibility);
            }
            // 전체 조회 권한(*)이 있으면 추가 가시성 필터 없이 모든 비삭제 채널 노출
        }

        // 3. 요청 조건 필터
        if (request.getChannelId() != null) {
            spec = spec.and(PostSpecs.hasChannelId(request.getChannelId()));
        }
        if (request.getAuthor() != null && !request.getAuthor().isBlank()) {
            spec = spec.and(PostSpecs.containsAuthor(request.getAuthor()));
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            spec = spec.and(PostSpecs.containsTitle(request.getTitle()));
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            spec = spec.and(PostSpecs.containsContent(request.getContent()));
        }
        if (request.getPostType() != null && !request.getPostType().isBlank()) {
            spec = spec.and(PostSpecs.hasPostType(PostType.valueOf(request.getPostType())));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            spec = spec.and(PostSpecs.hasStatus(PostStatus.valueOf(request.getStatus())));
        }
        if (request.getChannelType() != null && !request.getChannelType().isBlank()) {
            spec = spec.and(PostSpecs.hasChannelType(ChannelType.valueOf(request.getChannelType())));
        }
        if (request.getClassroomId() != null) {
            spec = spec.and(PostSpecs.hasChannelType(ChannelType.CLASSROOM))
                    .and(PostSpecs.hasChannelRefId(request.getClassroomId()));
        }
        if (request.getDepartmentId() != null) {
            spec = spec.and(PostSpecs.hasChannelType(ChannelType.DEPARTMENT))
                    .and(PostSpecs.hasChannelRefId(request.getDepartmentId()));
        }
        if (request.getIsPinned() != null) {
            spec = spec.and(PostSpecs.hasIsPinned(request.getIsPinned()));
        }
        return spec;
    }

    private boolean hasWildcardPostPermission(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("post:read:*") ||
                               a.getAuthority().startsWith("post:write:*") ||
                               a.getAuthority().startsWith("post:manage:*"));
    }

    private List<Long> extractAllowedChannelIds(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("post:read:") || a.startsWith("post:write:") || a.startsWith("post:manage:"))
                .map(a -> a.substring(a.lastIndexOf(":") + 1))
                .filter(id -> !id.equals("*"))
                .map(Long::valueOf)
                .distinct()
                .toList();
    }
}
