package geumjeongyahak.domain.file.enums;

import java.util.Arrays;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;

public enum DriveUploadTarget {
    HANDOVER("handover"),
    EXAM_MATERIALS("examMaterials"),
    DOCUMENT_FORMS("documentForms"),
    MEETING_RECORDS("meetingRecords"),
    BOARD("board");

    private final String path;

    DriveUploadTarget(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static DriveUploadTarget fromPath(String value) {
        return Arrays.stream(values())
            .filter(target -> target.path.equals(value))
            .findFirst()
            .orElseThrow(() -> new BadRequestException(CommonErrorCode.INVALID_INPUT, "지원하지 않는 Drive 업로드 대상입니다."));
    }
}
