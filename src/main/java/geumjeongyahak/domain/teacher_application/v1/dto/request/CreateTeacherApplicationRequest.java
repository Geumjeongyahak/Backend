package geumjeongyahak.domain.teacher_application.v1.dto.request;

import java.time.LocalDate;

import geumjeongyahak.common.validation.annotation.ValidEmail;
import geumjeongyahak.common.validation.annotation.ValidPhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Schema(description = "교원 신청서 제출 요청")
public record CreateTeacherApplicationRequest(

    @Schema(description = "생년월일", example = "1999-03-15")
    @NotNull(message = "생년월일은 필수입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    LocalDate birthDate,

    @Schema(description = "연락처", example = "010-0000-0000")
    @NotBlank(message = "연락처는 필수입니다.")
    @ValidPhoneNumber
    String phoneNumber,

    @Schema(description = "이메일", example = "hong@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @ValidEmail
    String email,

    @Schema(description = "주소", example = "부산광역시 금정구")
    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
    String address,

    @Schema(description = "최종 학력 및 전공", example = "부산대학교 국어국문학과 졸업")
    @NotBlank(message = "최종 학력 및 전공은 필수입니다.")
    @Size(max = 255, message = "최종 학력 및 전공은 255자 이하로 입력해주세요.")
    String educationAndMajor,

    @Schema(description = "선호 과목 ID", example = "3")
    @NotNull(message = "선호 과목 ID는 필수입니다.")
    Long preferredSubjectId,

    @Schema(description = "지원 동기")
    @NotBlank(message = "지원 동기는 필수입니다.")
    String motivation,

    @Schema(description = "희망하는 교사상")
    @NotBlank(message = "희망하는 교사상은 필수입니다.")
    String desiredTeacherImage,

    @Schema(description = "나눔의 의미")
    @NotBlank(message = "나눔의 의미는 필수입니다.")
    String meaningOfSharing
) {
}
