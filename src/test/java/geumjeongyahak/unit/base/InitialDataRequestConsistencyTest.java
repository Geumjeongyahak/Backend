package geumjeongyahak.unit.base;

import static geumjeongyahak.unit.base.InitialDataSqlReader.longValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import geumjeongyahak.domain.daily_schedule.enums.DailyTeacherAttendanceStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalStatus;
import geumjeongyahak.domain.request.enums.LessonExchangeProposalType;
import geumjeongyahak.domain.request.enums.LessonExchangeRequestStatus;
import geumjeongyahak.domain.request.enums.RequestStatus;

@DisplayName("초기 데이터 요청 정합성 테스트")
class InitialDataRequestConsistencyTest {

    private static final Path INITIAL_DATA = Path.of("src/main/resources/sql/init_data.sql");
    private static final Set<String> ACTIVE_REQUEST_STATUSES = Set.of(
        LessonExchangeRequestStatus.PENDING.name(),
        LessonExchangeRequestStatus.APPROVED.name()
    );

    @Test
    @DisplayName("승인된 결석 요청은 해당 일정의 교사 출석을 공결로 처리해야 한다")
    void approvedAbsenceRequests_haveExcusedTeacherAttendance() throws IOException {
        List<Map<String, String>> requests = readTable("absence_requests");
        Map<Long, Map<String, String>> attendancesByScheduleId = indexBy(
            readTable("daily_teacher_attendances"),
            row -> longValue(row, "daily_schedule_id")
        );
        List<String> errors = new ArrayList<>();
        requireRows(errors, "absence_requests", requests);
        requireRows(errors, "daily_teacher_attendances", attendancesByScheduleId.values());

        for (Map<String, String> request : requests) {
            Long requestId = longValue(request, "id");
            String status = request.get("status");
            validateAbsenceStatusFields(errors, requestId, status, request);

            if (!RequestStatus.APPROVED.name().equals(status)) {
                continue;
            }

            Long scheduleId = longValue(request, "daily_schedule_id");
            Map<String, String> attendance = attendancesByScheduleId.get(scheduleId);
            if (attendance == null) {
                errors.add("absence_request " + requestId + ": daily_schedule " + scheduleId + "의 출석이 없습니다.");
            } else if (!DailyTeacherAttendanceStatus.EXCUSED.name().equals(attendance.get("status"))) {
                errors.add("absence_request " + requestId + ": 승인 요청의 교사 출석이 EXCUSED가 아닙니다.");
            }
        }

        assertNoErrors(errors);
    }

    @Test
    @DisplayName("수업 교환 요청은 일정 정보, 활성 중복, 상태별 처리 필드가 일치해야 한다")
    void lessonExchangeRequests_followRequestPolicies() throws IOException {
        List<Map<String, String>> requests = readTable("lesson_exchange_requests");
        Map<Long, Map<String, String>> schedulesById = indexBy(
            readTable("daily_schedules"),
            row -> longValue(row, "id")
        );
        List<String> errors = new ArrayList<>();
        requireRows(errors, "lesson_exchange_requests", requests);
        requireRows(errors, "daily_schedules", schedulesById.values());

        Map<String, Long> activeRequestCounts = requests.stream()
            .filter(row -> ACTIVE_REQUEST_STATUSES.contains(row.get("status")))
            .collect(Collectors.groupingBy(
                row -> row.get("requested_by") + ":" + row.get("daily_schedule_id"),
                LinkedHashMap::new,
                Collectors.counting()
            ));
        activeRequestCounts.forEach((key, count) -> {
            if (count > 1) {
                errors.add("lesson_exchange_requests: 요청자와 일정이 같은 활성 요청이 중복됩니다. key=" + key);
            }
        });

        for (Map<String, String> request : requests) {
            Long requestId = longValue(request, "id");
            Long scheduleId = longValue(request, "daily_schedule_id");
            Map<String, String> schedule = schedulesById.get(scheduleId);
            if (schedule == null) {
                errors.add("lesson_exchange_request " + requestId + ": daily_schedule " + scheduleId + "가 없습니다.");
                continue;
            }

            compare(errors, requestId, "requested_by", schedule.get("teacher_id"), request.get("requested_by"));
            compare(errors, requestId, "lesson_date", schedule.get("lesson_date"), request.get("lesson_date"));
            validateExchangeRequestStatusFields(errors, requestId, request.get("status"), request);
        }

        assertNoErrors(errors);
    }

    @Test
    @DisplayName("수업 교환 제안은 유형, 활성 중복, 상태별 처리 필드와 제안자 일정이 일치해야 한다")
    void lessonExchangeProposals_followProposalPolicies() throws IOException {
        List<Map<String, String>> proposals = readTable("lesson_exchange_proposals");
        Map<Long, Map<String, String>> requestsById = indexBy(
            readTable("lesson_exchange_requests"),
            row -> longValue(row, "id")
        );
        Map<Long, Map<String, String>> schedulesById = indexBy(
            readTable("daily_schedules"),
            row -> longValue(row, "id")
        );
        List<String> errors = new ArrayList<>();
        requireRows(errors, "lesson_exchange_proposals", proposals);

        Map<String, Long> activeProposalCounts = proposals.stream()
            .filter(row -> LessonExchangeProposalStatus.ACTIVE.name().equals(row.get("status")))
            .collect(Collectors.groupingBy(
                row -> row.get("request_id") + ":" + row.get("proposed_by"),
                LinkedHashMap::new,
                Collectors.counting()
            ));
        activeProposalCounts.forEach((key, count) -> {
            if (count > 1) {
                errors.add("lesson_exchange_proposals: 요청과 제안자가 같은 활성 제안이 중복됩니다. key=" + key);
            }
        });

        for (Map<String, String> proposal : proposals) {
            Long proposalId = longValue(proposal, "id");
            Long requestId = longValue(proposal, "request_id");
            Map<String, String> request = requestsById.get(requestId);
            if (request == null) {
                errors.add("lesson_exchange_proposal " + proposalId + ": request " + requestId + "가 없습니다.");
                continue;
            }
            if (request.get("requested_by").equals(proposal.get("proposed_by"))) {
                errors.add("lesson_exchange_proposal " + proposalId + ": 요청자 본인이 제안자입니다.");
            }

            validateProposalTypeFields(errors, proposalId, proposal, schedulesById);
            validateProposalStatusFields(errors, proposalId, proposal.get("status"), proposal);
        }

        validateCompletedRequestProposals(errors, proposals, requestsById);
        assertNoErrors(errors);
    }

    private static void validateAbsenceStatusFields(
        List<String> errors,
        Long requestId,
        String status,
        Map<String, String> request
    ) {
        switch (RequestStatus.valueOf(status)) {
            case PENDING, CANCELLED, EXPIRED -> requireNull(
                errors, "absence_request", requestId, request, "approval_at", "approval_by", "note"
            );
            case APPROVED -> {
                requireNonNull(errors, "absence_request", requestId, request, "approval_at", "approval_by");
                requireNull(errors, "absence_request", requestId, request, "note");
            }
            case REJECTED -> requireNonNull(
                errors, "absence_request", requestId, request, "approval_at", "approval_by", "note"
            );
        }
    }

    private static void validateExchangeRequestStatusFields(
        List<String> errors,
        Long requestId,
        String status,
        Map<String, String> request
    ) {
        switch (LessonExchangeRequestStatus.valueOf(status)) {
            case PENDING, EXPIRED -> requireNull(
                errors, "lesson_exchange_request", requestId, request,
                "processed_at", "processed_by", "completed_at", "cancelled_at", "rejection_note"
            );
            case APPROVED -> {
                requireNonNull(errors, "lesson_exchange_request", requestId, request, "processed_at", "processed_by");
                requireNull(
                    errors, "lesson_exchange_request", requestId, request,
                    "completed_at", "cancelled_at", "rejection_note"
                );
            }
            case REJECTED -> {
                requireNonNull(
                    errors, "lesson_exchange_request", requestId, request,
                    "processed_at", "processed_by", "rejection_note"
                );
                requireNull(errors, "lesson_exchange_request", requestId, request, "completed_at", "cancelled_at");
            }
            case COMPLETED -> {
                requireNonNull(
                    errors, "lesson_exchange_request", requestId, request,
                    "processed_at", "processed_by", "completed_at"
                );
                requireNull(errors, "lesson_exchange_request", requestId, request, "cancelled_at", "rejection_note");
            }
            case CANCELLED -> {
                requireNonNull(errors, "lesson_exchange_request", requestId, request, "cancelled_at");
                requireNull(
                    errors, "lesson_exchange_request", requestId, request,
                    "processed_at", "processed_by", "completed_at", "rejection_note"
                );
            }
        }
    }

    private static void validateProposalTypeFields(
        List<String> errors,
        Long proposalId,
        Map<String, String> proposal,
        Map<Long, Map<String, String>> schedulesById
    ) {
        if (LessonExchangeProposalType.SUBSTITUTION.name().equals(proposal.get("proposal_type"))) {
            requireNull(
                errors, "lesson_exchange_proposal", proposalId, proposal,
                "daily_schedule_id", "lesson_date", "classroom_name_snapshot"
            );
            return;
        }

        requireNonNull(
            errors, "lesson_exchange_proposal", proposalId, proposal,
            "daily_schedule_id", "lesson_date", "classroom_name_snapshot"
        );
        Long scheduleId = longValue(proposal, "daily_schedule_id");
        Map<String, String> schedule = schedulesById.get(scheduleId);
        if (schedule == null) {
            errors.add("lesson_exchange_proposal " + proposalId + ": daily_schedule " + scheduleId + "가 없습니다.");
            return;
        }
        compare(errors, proposalId, "proposed_by", schedule.get("teacher_id"), proposal.get("proposed_by"));
        compare(errors, proposalId, "lesson_date", schedule.get("lesson_date"), proposal.get("lesson_date"));
    }

    private static void validateProposalStatusFields(
        List<String> errors,
        Long proposalId,
        String status,
        Map<String, String> proposal
    ) {
        switch (LessonExchangeProposalStatus.valueOf(status)) {
            case ACTIVE -> requireNull(
                errors, "lesson_exchange_proposal", proposalId, proposal,
                "accepted_at", "withdrawn_at", "closed_at"
            );
            case ACCEPTED -> {
                requireNonNull(errors, "lesson_exchange_proposal", proposalId, proposal, "accepted_at");
                requireNull(errors, "lesson_exchange_proposal", proposalId, proposal, "withdrawn_at", "closed_at");
            }
            case WITHDRAWN -> {
                requireNonNull(errors, "lesson_exchange_proposal", proposalId, proposal, "withdrawn_at");
                requireNull(errors, "lesson_exchange_proposal", proposalId, proposal, "accepted_at", "closed_at");
            }
            case CLOSED -> {
                requireNonNull(errors, "lesson_exchange_proposal", proposalId, proposal, "closed_at");
                requireNull(errors, "lesson_exchange_proposal", proposalId, proposal, "accepted_at", "withdrawn_at");
            }
        }
    }

    private static void validateCompletedRequestProposals(
        List<String> errors,
        List<Map<String, String>> proposals,
        Map<Long, Map<String, String>> requestsById
    ) {
        requestsById.values().stream()
            .filter(request -> LessonExchangeRequestStatus.COMPLETED.name().equals(request.get("status")))
            .forEach(request -> {
                Long requestId = longValue(request, "id");
                long acceptedCount = proposals.stream()
                    .filter(proposal -> requestId.equals(longValue(proposal, "request_id")))
                    .filter(proposal -> LessonExchangeProposalStatus.ACCEPTED.name().equals(proposal.get("status")))
                    .count();
                if (acceptedCount != 1) {
                    errors.add("lesson_exchange_request " + requestId + ": 완료 요청의 수락 제안은 1개여야 합니다.");
                }
            });
    }

    private static List<Map<String, String>> readTable(String tableName) throws IOException {
        return InitialDataSqlReader.readTable(INITIAL_DATA, tableName);
    }

    private static <T> Map<Long, T> indexBy(List<T> rows, Function<T, Long> idExtractor) {
        return rows.stream().collect(Collectors.toMap(
            idExtractor,
            Function.identity(),
            (first, second) -> second,
            LinkedHashMap::new
        ));
    }

    private static void requireRows(List<String> errors, String table, Iterable<?> rows) {
        if (!rows.iterator().hasNext()) {
            errors.add(table + " 초기 데이터를 찾을 수 없습니다.");
        }
    }

    private static void requireNull(
        List<String> errors,
        String table,
        Long rowId,
        Map<String, String> row,
        String... columns
    ) {
        for (String column : columns) {
            if (row.get(column) != null) {
                errors.add(table + " " + rowId + ": " + column + "는 NULL이어야 합니다.");
            }
        }
    }

    private static void requireNonNull(
        List<String> errors,
        String table,
        Long rowId,
        Map<String, String> row,
        String... columns
    ) {
        for (String column : columns) {
            if (row.get(column) == null) {
                errors.add(table + " " + rowId + ": " + column + "가 필요합니다.");
            }
        }
    }

    private static void compare(List<String> errors, Long rowId, String field, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            errors.add("row " + rowId + ": " + field + "가 다릅니다. expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertNoErrors(List<String> errors) {
        assertTrue(
            errors.isEmpty(),
            () -> INITIAL_DATA + System.lineSeparator() + String.join(System.lineSeparator(), errors)
        );
    }
}
