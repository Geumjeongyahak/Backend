package geumjeongyahak.domain.post.v1.controller;

import geumjeongyahak.common.security.service.CustomUserDetails;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.post.service.PostAdminViewService;
import geumjeongyahak.domain.post.v1.dto.response.PostDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PostViewController {

    private final PostAdminViewService postAdminViewService;

    @GetMapping("/admin/post/posts")
    public String allPosts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        PostAdminViewService.PostFilter filter = new PostAdminViewService.PostFilter(keyword, status, page, size, sort);
        model.addAttribute("active", "posts");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("filter", filter);
        model.addAttribute("postsPage", postAdminViewService.getPosts(null, filter));
        model.addAttribute("postStatuses", postAdminViewService.getStatuses());
        return "admin/post/posts";
    }

    @GetMapping("/admin/channel/{channelId}/posts")
    public String channelPosts(
        @PathVariable Long channelId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sort,
        Model model,
        Authentication authentication
    ) {
        PostAdminViewService.PostFilter filter = new PostAdminViewService.PostFilter(keyword, status, page, size, sort);
        model.addAttribute("active", "channels");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("channelId", channelId);
        model.addAttribute("filter", filter);
        model.addAttribute("postsPage", postAdminViewService.getPosts(channelId, filter));
        model.addAttribute("postStatuses", postAdminViewService.getStatuses());
        return "admin/channel/post/posts";
    }

    @PostMapping("/admin/channel/{channelId}/posts/drafts")
    public String createDraft(
        @PathVariable Long channelId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        Long postId = postAdminViewService.createDraft(channelId, userDetails);
        redirectAttributes.addFlashAttribute("message", "초안을 생성했습니다. 이미지와 첨부파일을 추가한 뒤 발행하세요.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId + "/edit";
    }

    @PostMapping("/admin/channel/{channelId}/posts")
    public String createPost(
        @PathVariable Long channelId,
        @RequestParam String title,
        @RequestParam String contentHtml,
        @RequestParam(required = false, defaultValue = "PUBLISHED") String status,
        @RequestParam(required = false, defaultValue = "false") Boolean isPinned,
        @RequestParam(required = false, defaultValue = "false") Boolean allowComment,
        @RequestParam(required = false) String thumbnailUrl,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        Long postId = postAdminViewService.createPost(channelId, userDetails, title, contentHtml, status, isPinned, allowComment, thumbnailUrl);
        redirectAttributes.addFlashAttribute("message", "게시글을 생성했습니다. 상세 화면에서 이미지와 첨부파일을 추가할 수 있습니다.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId;
    }

    @GetMapping("/admin/channel/{channelId}/posts/{postId}")
    public String postDetail(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        Model model,
        Authentication authentication,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PostDetailResponse post = postAdminViewService.getPost(channelId, postId);
        model.addAttribute("active", "channels");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("currentUserId", userDetails.getUserId());
        model.addAttribute("post", post);
        model.addAttribute("comments", postAdminViewService.getComments(channelId, postId));
        return "admin/channel/post/posts-detail";
    }

    @GetMapping("/admin/channel/{channelId}/posts/{postId}/edit")
    public String editPost(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        Model model,
        Authentication authentication
    ) {
        model.addAttribute("active", "channels");
        model.addAttribute("adminName", authentication.getName());
        model.addAttribute("post", postAdminViewService.getPost(channelId, postId));
        model.addAttribute("postStatuses", postAdminViewService.getStatuses());
        return "admin/channel/post/posts-edit";
    }

    @PostMapping("/admin/channel/{channelId}/posts/{postId}")
    public String updatePost(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @RequestParam String title,
        @RequestParam String contentHtml,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "false") Boolean allowComment,
        @RequestParam(required = false) String thumbnailUrl,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        if ("DRAFT".equals(status)) {
            postAdminViewService.saveDraft(channelId, postId, userDetails, title, contentHtml, allowComment, thumbnailUrl);
            redirectAttributes.addFlashAttribute("message", "초안을 임시 저장했습니다.");
            return "redirect:/admin/channel/" + channelId + "/posts/" + postId + "/edit";
        }
        postAdminViewService.updatePost(channelId, userDetails, postId, title, contentHtml, status, allowComment, thumbnailUrl);
        redirectAttributes.addFlashAttribute("message", "게시글을 수정했습니다.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId;
    }

    @PostMapping("/admin/channel/{channelId}/posts/{postId}/publish")
    public String publishDraft(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @RequestParam String title,
        @RequestParam String contentHtml,
        @RequestParam(required = false, defaultValue = "false") Boolean allowComment,
        @RequestParam(required = false) String thumbnailUrl,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        postAdminViewService.publishDraft(channelId, postId, userDetails, title, contentHtml, allowComment, thumbnailUrl);
        redirectAttributes.addFlashAttribute("message", "게시글을 발행했습니다.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId;
    }

    @PostMapping("/admin/channel/{channelId}/posts/{postId}/delete")
    public String deletePost(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @RequestParam(required = false) String returnTo,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        postAdminViewService.deletePost(channelId, userDetails, postId);
        redirectAttributes.addFlashAttribute("message", "게시글을 삭제했습니다.");
        if ("posts".equals(returnTo)) {
            return "redirect:/admin/post/posts";
        }
        return "redirect:/admin/channel/" + channelId + "/posts";
    }

    @PostMapping("/admin/channel/{channelId}/posts/{postId}/comments")
    public String createComment(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @RequestParam String content,
        @RequestParam(required = false) Long parentCommentId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        postAdminViewService.createComment(channelId, postId, userDetails, content, parentCommentId);
        redirectAttributes.addFlashAttribute("message", "댓글을 등록했습니다.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId;
    }

    @PostMapping("/admin/channel/{channelId}/posts/{postId}/comments/{commentId}")
    public String updateComment(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @RequestParam String content,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        postAdminViewService.updateComment(channelId, postId, commentId, userDetails, content);
        redirectAttributes.addFlashAttribute("message", "댓글을 수정했습니다.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId;
    }

    @PostMapping("/admin/channel/{channelId}/posts/{postId}/comments/{commentId}/delete")
    public String deleteComment(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes
    ) {
        postAdminViewService.deleteComment(channelId, postId, commentId, userDetails);
        redirectAttributes.addFlashAttribute("message", "댓글을 삭제했습니다.");
        return "redirect:/admin/channel/" + channelId + "/posts/" + postId;
    }

    @ResponseBody
    @PostMapping("/admin/channel/{channelId}/posts/{postId}/images")
    public ResponseEntity<FileUploadResponse> uploadImage(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(postAdminViewService.uploadImage(channelId, postId, file));
    }

    @ResponseBody
    @PostMapping("/admin/channel/{channelId}/posts/{postId}/attachments")
    public ResponseEntity<FileUploadResponse> uploadAttachment(
        @PathVariable Long channelId,
        @PathVariable Long postId,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(postAdminViewService.uploadAttachment(channelId, postId, file));
    }
}
