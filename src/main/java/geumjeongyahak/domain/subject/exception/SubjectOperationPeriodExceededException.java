package geumjeongyahak.domain.subject.exception;

import geumjeongyahak.common.exception.BadRequestException;

public class SubjectOperationPeriodExceededException extends BadRequestException {

    public SubjectOperationPeriodExceededException(long maxDays) {
        super(SubjectErrorCode.INVALID_SUBJECT_SCHEDULE, "과목 운영 기간은 최대 " + maxDays + "일까지 설정할 수 있습니다.");
    }
}
