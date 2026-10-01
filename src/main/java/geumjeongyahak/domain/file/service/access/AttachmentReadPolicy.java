package geumjeongyahak.domain.file.service.access;

import java.util.UUID;

import geumjeongyahak.common.security.service.CustomUserDetails;

/**
 * 첨부파일을 붙이는 도메인이 «내 리소스에 붙은 파일을 누가 받을 수 있나»를 내놓는 창구.
 *
 * <p>{@code files}에는 올린 사람이 없어서, 파일이 붙은 리소스를 읽을 수 있으면 받을 수 있다고 본다.
 * 새 첨부 위치가 생기면 그 도메인에 구현체({@code @Component}) 하나를 더한다.
 * {@link geumjeongyahak.domain.file.service.AttachmentUploadService}는 ADMIN이거나 구현체 중 하나라도 true면 허용한다.
 */
public interface AttachmentReadPolicy {

    /**
     * @param fileId 다운로드하려는 파일
     * @param user   요청한 사용자. 비로그인이면 {@code null}
     * @return 이 파일이 내 리소스에 붙어 있고, 그 리소스를 {@code user}가 읽을 수 있으면 true
     */
    boolean canRead(UUID fileId, CustomUserDetails user);
}
