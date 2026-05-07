package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.base.dto.response.AdminPage;
import geumjeongyahak.domain.base.dto.response.AdminSorts;
import geumjeongyahak.domain.comment.service.CommentCrudService;
import geumjeongyahak.domain.comment.v1.dto.request.CreateCommentRequest;
import geumjeongyahak.domain.comment.v1.dto.request.UpdateCommentRequest;
import geumjeongyahak.domain.comment.v1.dto.response.CommentResponse;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.service.AttachmentUploadService;
import geumjeongyahak.domain.file.service.ImageUploadService;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.post.entity.Post;
import geumjeongyahak.domain.post.entity.PostAttachment;
import geumjeongyahak.domain.post.entity.PostFile;
import geumjeongyahak.domain.post.enums.PostStatus;
import geumjeongyahak.domain.post.exception.PostErrorCode;
import geumjeongyahak.domain.post.repository.PostAttachmentRepository;
import geumjeongyahak.domain.post.repository.PostFileRepository;
import geumjeongyahak.domain.post.repository.PostRepository;
import geumjeongyahak.domain.post.v1.dto.request.CreatePostRequest;
import geumjeongyahak.domain.post.v1.dto.request.UpdatePostRequest;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAdminViewService {

    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final FileRepository fileRepository;
    private final ImageUploadService imageUploadService;
    private final AttachmentUploadService attachmentUploadService;
    private final PostCrudService postCrudService;
    private final PostActionService postActionService;
    private final CommentCrudService commentCrudService;

    public AdminPage<AdminPostRow> getPosts(Long channelId, PostFilter filter) {
        List<AdminPostRow> rows = postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .filter(post -> !post.isDeleted())
            .filter(post -> channelId == null || post.belongsTo(channelId))
            .filter(post -> matchesKeyword(post, filter.keyword()))
            .filter(post -> isBlank(filter.status()) || post.getStatus().name().equals(filter.status()))
            .map(AdminPostRow::from)
            .toList();

        return AdminPage.from(sortPosts(rows, filter.sort()), filter.page(), filter.size());
    }

    public PostDetailResponse getPost(Long channelId, Long postId) {
        return postRepository.findWithAttachmentsByIdAndChannelId(postId, channelId)
            .filter(post -> !post.isDeleted())
            .map(PostDetailResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    @Transactional
    public Long createPost(
        Long channelId,
        CustomUserDetails userDetails,
        String title,
        String contentHtml,
        String status,
        Boolean isPinned,
        Boolean allowComment,
        String thumbnailUrl
    ) {
        return postCrudService.createPost(channelId, userDetails, new CreatePostRequest(
            title,
            contentHtml,
            status,
            isPinned,
            allowComment,
            thumbnailUrl
        )).id();
    }

    @Transactional
    public Long createDraft(Long channelId, CustomUserDetails userDetails) {
        return postActionService.createDraft(channelId, userDetails).id();
    }

    @Transactional
    public void saveDraft(
        Long channelId,
        Long postId,
        CustomUserDetails userDetails,
        String title,
        String contentHtml,
        Boolean allowComment,
        String thumbnailUrl
    ) {
        postActionService.saveDraft(channelId, postId, userDetails, new PostActionService.SaveDraftCommand(
            title,
            contentHtml,
            allowComment,
            thumbnailUrl
        ));
    }

    @Transactional
    public void publishDraft(
        Long channelId,
        Long postId,
        CustomUserDetails userDetails,
        String title,
        String contentHtml,
        Boolean allowComment,
        String thumbnailUrl
    ) {
        postActionService.publish(channelId, postId, userDetails, new PostActionService.PublishPostCommand(
            title,
            contentHtml,
            allowComment,
            thumbnailUrl
        ));
    }

    @Transactional
    public void updatePost(
        Long channelId,
        CustomUserDetails userDetails,
        Long postId,
        String title,
        String contentHtml,
        String status,
        Boolean allowComment,
        String thumbnailUrl
    ) {
        postCrudService.updatePost(channelId, userDetails, postId, new UpdatePostRequest(
            title,
            contentHtml,
            status,
            allowComment,
            thumbnailUrl
        ));
    }

    public List<CommentResponse> getComments(Long channelId, Long postId) {
        return commentCrudService.getComments(channelId, postId);
    }

    @Transactional
    public void createComment(Long channelId, Long postId, CustomUserDetails userDetails, String content, Long parentCommentId) {
        commentCrudService.createComment(channelId, postId, userDetails, new CreateCommentRequest(content, parentCommentId));
    }

    @Transactional
    public void updateComment(Long channelId, Long postId, Long commentId, CustomUserDetails userDetails, String content) {
        commentCrudService.updateComment(channelId, postId, commentId, userDetails, new UpdateCommentRequest(content));
    }

    @Transactional
    public void deleteComment(Long channelId, Long postId, Long commentId, CustomUserDetails userDetails) {
        commentCrudService.deleteComment(channelId, postId, commentId, userDetails);
    }

    @Transactional
    public FileUploadResponse uploadImage(Long channelId, Long postId, MultipartFile multipartFile) {
        Post post = getActivePost(channelId, postId);
        FileUploadResponse uploaded = imageUploadService.uploadPostImage(multipartFile);
        File file = fileRepository.findById(uploaded.fileId())
            .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        if (!postFileRepository.existsByPostIdAndFileId(postId, file.getId())) {
            int nextSortOrder = Math.toIntExact(postFileRepository.countByPostId(postId));
            postFileRepository.save(PostFile.builder()
                .post(post)
                .file(file)
                .sortOrder(nextSortOrder)
                .build());
        }

        return uploaded;
    }

    @Transactional
    public FileUploadResponse uploadAttachment(Long channelId, Long postId, MultipartFile multipartFile) {
        Post post = getActivePost(channelId, postId);
        FileUploadResponse uploaded = attachmentUploadService.uploadAttachment(multipartFile);
        File file = fileRepository.findById(uploaded.fileId())
            .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        if (!postAttachmentRepository.existsByPostIdAndFileId(postId, file.getId())) {
            int nextSortOrder = Math.toIntExact(postAttachmentRepository.countByPostId(postId));
            postAttachmentRepository.save(PostAttachment.builder()
                .post(post)
                .file(file)
                .sortOrder(nextSortOrder)
                .build());
        }

        return uploaded;
    }

    @Transactional
    public void deleteAttachment(Long channelId, Long postId, UUID fileId) {
        getActivePost(channelId, postId);
        PostAttachment attachment = postAttachmentRepository.findByPostIdAndFileId(postId, fileId)
            .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        postAttachmentRepository.delete(attachment);
        attachmentUploadService.deleteAttachmentIfPresent(fileId);
    }

    @Transactional
    public void deletePost(Long channelId, CustomUserDetails userDetails, Long postId) {
        postCrudService.deletePost(channelId, userDetails, postId);
    }

    private Post getActivePost(Long channelId, Long postId) {
        return postRepository.findByIdAndChannelId(postId, channelId)
            .filter(post -> !post.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private List<AdminPostRow> sortPosts(List<AdminPostRow> rows, String sort) {
        return AdminSorts.sort(rows, sort, Map.of(
            "id", Comparator.comparing(AdminPostRow::id),
            "title", Comparator.comparing(AdminPostRow::title, Comparator.nullsLast(String::compareToIgnoreCase)),
            "channelName", Comparator.comparing(AdminPostRow::channelName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "status", Comparator.comparing(AdminPostRow::status, Comparator.nullsLast(String::compareToIgnoreCase)),
            "authorName", Comparator.comparing(AdminPostRow::authorName, Comparator.nullsLast(String::compareToIgnoreCase)),
            "viewCount", Comparator.comparing(AdminPostRow::viewCount),
            "createdAt", Comparator.comparing(AdminPostRow::createdAt, Comparator.nullsLast(LocalDateTime::compareTo))
        ), "createdAt,DESC");
    }

    private boolean matchesKeyword(Post post, String keyword) {
        if (isBlank(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(post.getTitle(), normalized)
            || contains(post.getContentHtml(), normalized)
            || contains(post.getAuthor().getName(), normalized)
            || contains(post.getChannel().getName(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public PostStatus[] getStatuses() {
        return PostStatus.values();
    }

    public record PostFilter(
        String keyword,
        String status,
        Integer page,
        Integer size,
        String sort
    ) {
    }

    public record AdminPostRow(
        Long id,
        Long channelId,
        String channelName,
        String title,
        String status,
        Long authorId,
        String authorName,
        boolean isPinned,
        boolean allowComment,
        long viewCount,
        LocalDateTime createdAt
    ) {
        private static AdminPostRow from(Post post) {
            return new AdminPostRow(
                post.getId(),
                post.getChannel().getId(),
                post.getChannel().getName(),
                post.getTitle(),
                post.getStatus().name(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.isPinned(),
                post.isAllowComment(),
                post.getViewCount(),
                post.getCreatedAt()
            );
        }
    }
}
