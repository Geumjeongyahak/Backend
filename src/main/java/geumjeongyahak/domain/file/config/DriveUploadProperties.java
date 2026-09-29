package geumjeongyahak.domain.file.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.file.drive")
public class DriveUploadProperties {

    private String sharedDriveId;
    private final FolderIds folderIds = new FolderIds();
    private final OAuth oauth = new OAuth();
    private boolean makeLinkPublic = false;
    private String uploadBaseUrl = "https://www.googleapis.com/upload/drive/v3";
    private String apiBaseUrl = "https://www.googleapis.com/drive/v3";
    // 파일 정보 조회는 DB 트랜잭션 안에서 불린다. 없으면 Drive 가 멈췄을 때 DB 연결이 끝없이 묶인다
    private Duration metadataTimeout = Duration.ofSeconds(10);

    public String folderIdFor(DriveUploadTarget target) {
        String folderId = switch (target) {
            case HANDOVER -> folderIds.getHandover();
            case EXAM_MATERIALS -> folderIds.getExamMaterials();
            case DOCUMENT_FORMS -> folderIds.getDocumentForms();
            case MEETING_RECORDS -> folderIds.getMeetingRecords();
            case BOARD -> folderIds.getBoard();
        };
        if (!StringUtils.hasText(folderId)) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive 폴더 ID가 설정되지 않았습니다.");
        }
        return folderId.trim();
    }

    @Getter
    @Setter
    public static class FolderIds {
        private String handover;
        private String examMaterials;
        private String documentForms;
        private String meetingRecords;
        private String board;
    }

    @Getter
    @Setter
    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String refreshToken;
    }
}
