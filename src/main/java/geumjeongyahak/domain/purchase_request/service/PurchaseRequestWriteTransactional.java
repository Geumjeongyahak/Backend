package geumjeongyahak.domain.purchase_request.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구입 요청 행을 잠그고 상태나 내용을 바꾸는 트랜잭션.
 *
 * <p>락 대기를 포함해 5초 안에 끝나야 한다. 넘기면 트랜잭션을 되돌리고 DB 연결을 놓는다.
 * 이 애너테이션을 단 메서드는 구입 요청을 락 있는 조회로 읽는다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Transactional(timeout = 5)
public @interface PurchaseRequestWriteTransactional {
}
