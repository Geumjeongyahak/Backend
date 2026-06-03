package geumjeongyahak.domain.teacher_application.service;

import static java.util.stream.Collectors.groupingBy;

import geumjeongyahak.domain.subject.entity.Subject;
import geumjeongyahak.domain.subject.service.SubjectProxyService;
import geumjeongyahak.domain.teacher_application.service.dto.ScheduleGroupKey;
import geumjeongyahak.domain.teacher_application.v1.dto.response.AvailableTeacherScheduleResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailableTeacherScheduleService {

    private final SubjectProxyService subjectProxyService;

    public List<AvailableTeacherScheduleResponse> getAvailableTeacherSchedules() {
        log.debug("교원 신청 가능 시간표 목록 조회 요청");

        Map<ScheduleGroupKey, List<Subject>> subjectsBySchedule = subjectProxyService
            .getUnassignedActiveSubjectsOrderByStartAtAndId()
            .stream()
            .collect(groupingBy(ScheduleGroupKey::from));

        return subjectsBySchedule.entrySet()
            .stream()
            .map(entry -> AvailableTeacherScheduleResponse.from(entry.getKey(), entry.getValue()))
            .sorted(Comparator
                .comparing(AvailableTeacherScheduleResponse::startAt)
                .thenComparing(AvailableTeacherScheduleResponse::classroomName)
                .thenComparing(response -> response.dayOfWeek().getValue())
                .thenComparing(AvailableTeacherScheduleResponse::startTime))
            .toList();
    }
}
