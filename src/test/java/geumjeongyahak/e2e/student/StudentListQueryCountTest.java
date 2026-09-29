package geumjeongyahak.e2e.student;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import geumjeongyahak.domain.student.entity.Student;
import geumjeongyahak.domain.student.v1.dto.request.CreateStudentRequest;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CollectionStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("E2E: 학생 목록 쿼리 수 테스트")
class StudentListQueryCountTest extends StudentBaseTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("학생 목록은 학생마다 분반을 따로 읽지 않는다")
    void studentList_doesNotFetchClassroomsPerStudent() {
        for (int i = 0; i < 5; i++) {
            createQueryCountStudent("쿼리 수 검증 학생 " + i, "010-7000-000" + i);
        }
        // 인증 과정에서 사용자 권한 컬렉션을 읽으므로, 학생 분반 컬렉션만 센다
        CollectionStatistics classrooms = entityManagerFactory.unwrap(SessionFactory.class)
            .getStatistics()
            .getCollectionStatistics(Student.class.getName() + ".studentClassrooms");
        long before = classrooms.getFetchCount();

        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .get()
            .then()
            .statusCode(200);

        assertThat(classrooms.getFetchCount() - before).isZero();
    }

    private void createQueryCountStudent(String name, String phoneNumber) {
        given()
            .header(AUTH_HEADER, getAuthHeader(adminAccessToken))
            .contentType(ContentType.JSON)
            .body(new CreateStudentRequest(name, phoneNumber, "쿼리 수 검증", List.of(DEFAULT_CLASSROOM_ID)))
            .post()
            .then()
            .statusCode(201);
    }
}
