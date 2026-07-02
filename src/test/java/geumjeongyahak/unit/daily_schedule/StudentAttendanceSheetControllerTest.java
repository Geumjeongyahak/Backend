package geumjeongyahak.unit.daily_schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import geumjeongyahak.domain.daily_schedule.service.DailyScheduleService;
import geumjeongyahak.domain.daily_schedule.v1.controller.StudentAttendanceSheetController;
import geumjeongyahak.domain.daily_schedule.v1.dto.request.StudentAttendanceSheetRequest;
import geumjeongyahak.domain.daily_schedule.v1.dto.response.StudentAttendanceSheetResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceSheetControllerTest {

    @Mock
    private DailyScheduleService dailyScheduleService;

    @Test
    void getStudentAttendanceSheet_returnsServiceResponse() {
        StudentAttendanceSheetController controller = new StudentAttendanceSheetController(dailyScheduleService);
        StudentAttendanceSheetRequest request = new StudentAttendanceSheetRequest(2026, 2, 1L);
        StudentAttendanceSheetResponse serviceResponse = StudentAttendanceSheetResponse.of(
            2026,
            2,
            1L,
            "해바라기반",
            List.of(),
            List.of()
        );
        given(dailyScheduleService.getStudentAttendanceSheet(request)).willReturn(serviceResponse);

        ResponseEntity<StudentAttendanceSheetResponse> response = controller.getStudentAttendanceSheet(request);

        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(dailyScheduleService).getStudentAttendanceSheet(request);
    }
}
