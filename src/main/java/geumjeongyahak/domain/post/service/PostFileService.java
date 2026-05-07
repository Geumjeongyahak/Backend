package geumjeongyahak.domain.post.service;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.common.security.service.CustomUserDetails;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostFileService {

    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final FileRepository fileRepository;
    private final ImageUploadService imageUploadService;
    private final AttachmentUploadService attachmentUploadService;

    @Transactional
    public FileUploadResponse attachImage(Long channelId, Long postId, CustomUserDetails userDetails, MultipartFile file) {
        Post post = getOwnedDraft(channelId, postId, userDetails);

        FileUploadResponse uploaded = imageUploadService.uploadPostImage(file);
        File savedFile = fileRepository.findById(uploaded.fileId())
                .orElseThrow(() -> new ResourceNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (!postFileRepository.existsByPostIdAndFileId(postId, savedFile.getId())) {
            int nextSortOrder = Math.toIntExact(postFileRepository.countByPostId(postId));
            postFileRepository.save(PostFile.builder()
                    .post(post)
                    .file(savedFile)
                    .sortOrder(nextSortOrder)
                    .build());
        }

        log.info("게시글 이미지 연동 완료 - postId: {}, fileId: {}", postId, savedFile.getId());
        return uploaded;
    }

    @Transactional
    public FileUploadResponse attachAttachment(Long channelId, Long postId, CustomUserDetails userDetails, MultipartFile file) {
        Post post = getOwnedDraft(channelId, postId, userDetails);

        FileUploadResponse uploaded = attachmentUploadService.uploadAttachment(file);
        File savedFile = fileRepository.findById(uploaded.fileId())
                .orElseThrow(() -> new ResourceNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (!postAttachmentRepository.existsByPostIdAndFileId(postId, savedFile.getId())) {
            int nextSortOrder = Math.toIntExact(postAttachmentRepository.countByPostId(postId));
            postAttachmentRepository.save(PostAttachment.builder()
                    .post(post)
                    .file(savedFile)
                    .sortOrder(nextSortOrder)
                    .build());
        }

        log.info("게시글 첨부파일 연동 완료 - postId: {}, fileId: {}", postId, savedFile.getId());
        return uploaded;
    }

    @Transactional
    public void detachAttachment(Long channelId, Long postId, CustomUserDetails userDetails, UUID fileId) {
        getOwnedDraft(channelId, postId, userDetails);

        PostAttachment attachment = postAttachmentRepository.findByPostIdAndFileId(postId, fileId)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        postAttachmentRepository.delete(attachment);
        attachmentUploadService.deleteAttachment(fileId);

        log.info("게시글 첨부파일 삭제 완료 - postId: {}, fileId: {}", postId, fileId);
    }

    private Post getOwnedDraft(Long channelId, Long postId, CustomUserDetails userDetails) {
        Post post = postRepository.findByIdAndChannelId(postId, channelId)
                .orElseThrow(() -> new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND));

        if (post.isDeleted() || !post.belongsTo(channelId)) {
            throw new ResourceNotFoundException(PostErrorCode.POST_NOT_FOUND);
        }
        if (!post.getAuthor().getId().equals(userDetails.getUserId())) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "본인이 작성한 게시글만 파일을 수정할 수 있습니다.");
        }
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "초안 상태 게시글만 파일을 수정할 수 있습니다.");
        }
        return post;
    }
}

