package geumjeongyahak.domain.subject.event;

import geumjeongyahak.common.event.dto.BaseEventDto;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * 시간표 기간 복사로 담당 교사가 있는 과목들이 한꺼번에 생겼다 (#242).
 * 과목마다 {@link SubjectCreatedEvent}를 내면 같은 (분반, 날짜)를 교시 수만큼 동기화하므로 한 이벤트로 묶는다.
 */
@Getter
public class SubjectsCopiedEvent extends BaseEventDto {

    private final List<CopiedSubject> subjects;

    public SubjectsCopiedEvent(List<CopiedSubject> subjects) {
        this.subjects = List.copyOf(subjects);
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of("subjectIds", subjects.stream().map(CopiedSubject::subjectId).toList());
    }

    /** 수업을 만들 과목 하나. startAt은 이미 «오늘 이후»로 맞춰져 있다. */
    public record CopiedSubject(
        Long subjectId,
        Long teacherId,
        LocalDate startAt,
        LocalDate endAt,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer period
    ) {
    }
}
