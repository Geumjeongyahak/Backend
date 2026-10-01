package geumjeongyahak.domain.subject.exception;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ProblemDetailProperties;
import geumjeongyahak.domain.subject.v1.dto.response.SubjectCopyFailure;
import java.util.List;
import java.util.Map;

/** 복사할 과목 중 하나라도 검증에 실패했다. 실패 목록을 응답의 {@code failures}로 내보낸다. */
public class SubjectCopyConflictException extends BusinessException implements ProblemDetailProperties {

    private final List<SubjectCopyFailure> failures;

    public SubjectCopyConflictException(List<SubjectCopyFailure> failures) {
        super(SubjectErrorCode.SUBJECT_COPY_CONFLICT);
        this.failures = List.copyOf(failures);
    }

    @Override
    public Map<String, Object> problemProperties() {
        return Map.of("failures", failures);
    }
}
