package geumjeongyahak.common.exception;

import java.util.Map;

/**
 * 오류 응답(ProblemDetail)에 code 말고도 실어 보낼 값이 있는 예외가 구현한다.
 * 전역 처리기가 {@link #problemProperties()}를 그대로 속성으로 붙인다.
 */
public interface ProblemDetailProperties {

    Map<String, Object> problemProperties();
}
