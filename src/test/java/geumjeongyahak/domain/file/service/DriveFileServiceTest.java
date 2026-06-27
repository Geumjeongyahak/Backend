package geumjeongyahak.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.domain.classroom.entity.Classroom;
import geumjeongyahak.domain.classroom.enums.ClassroomType;
import geumjeongyahak.domain.classroom.service.ClassroomProxyService;
import geumjeongyahak.domain.department.entity.Department;
import geumjeongyahak.domain.department.service.DepartmentProxyService;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;

class DriveFileServiceTest {

    private final ClassroomProxyService classroomProxyService = mock(ClassroomProxyService.class);
    private final DepartmentProxyService departmentProxyService = mock(DepartmentProxyService.class);
    private final DriveFileService service = new DriveFileService(
        null,
        null,
        null,
        classroomProxyService,
        departmentProxyService
    );

    @Test
    void resolveFolderPath_boardWithoutScope_usesCommonYearMonth() {
        assertThat(service.resolveFolderPath(DriveUploadTarget.BOARD, null, null))
            .isEqualTo(List.of("공통", year(), month()));
    }

    @Test
    void resolveFolderPath_boardWithClassroomScope_usesClassroomName() {
        when(classroomProxyService.getActiveById(1L)).thenReturn(Classroom.builder()
            .name("벚꽃반")
            .type(ClassroomType.WEEKDAY)
            .build());

        assertThat(service.resolveFolderPath(DriveUploadTarget.BOARD, "classroom", 1L))
            .isEqualTo(List.of("반별", "벚꽃반", year(), month()));
    }

    @Test
    void resolveFolderPath_boardWithDepartmentScope_usesDepartmentName() {
        when(departmentProxyService.getById(2L)).thenReturn(Department.builder()
            .name("교육연구부")
            .description("교육")
            .build());

        assertThat(service.resolveFolderPath(DriveUploadTarget.BOARD, "department", 2L))
            .isEqualTo(List.of("부서별", "교육연구부", year(), month()));
    }

    @Test
    void resolveFolderPath_meetingRecords_usesYearMonthOnly() {
        assertThat(service.resolveFolderPath(DriveUploadTarget.MEETING_RECORDS, null, null))
            .isEqualTo(List.of(year(), month()));
    }

    @Test
    void resolveFolderPath_invalidScopeCombination_throwsBadRequest() {
        assertThatThrownBy(() -> service.resolveFolderPath(DriveUploadTarget.BOARD, "classroom", null))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.resolveFolderPath(DriveUploadTarget.BOARD, null, 1L))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.resolveFolderPath(DriveUploadTarget.BOARD, "student", 1L))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.resolveFolderPath(DriveUploadTarget.HANDOVER, "classroom", 1L))
            .isInstanceOf(BadRequestException.class);
    }

    private String year() {
        return Integer.toString(now().getYear());
    }

    private String month() {
        return String.format("%02d", now().getMonthValue());
    }

    private YearMonth now() {
        return YearMonth.now(ZoneId.of("Asia/Seoul"));
    }
}
