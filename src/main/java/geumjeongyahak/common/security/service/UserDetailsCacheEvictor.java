package geumjeongyahak.common.security.service;

import geumjeongyahak.domain.auth.entity.UserCredential;
import geumjeongyahak.domain.department.entity.DepartmentPermission;
import geumjeongyahak.domain.users.entity.User;
import geumjeongyahak.domain.users.entity.UserPermission;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 사용자·권한 엔티티가 바뀌면 커밋 뒤 userDetails 캐시를 지운다.
 * User · UserPermission · UserCredential · DepartmentPermission 에 @EntityListeners 로 붙는다.
 *
 * 벌크 JPQL·네이티브 UPDATE/DELETE 는 엔티티 콜백을 우회하므로 여기서 안 지워진다 —
 * 그런 코드를 더하면 그 자리에서 직접 지운다.
 */
@Component
@RequiredArgsConstructor
public class UserDetailsCacheEvictor {

    public static final String CACHE = "userDetails";

    private final CacheManager cacheManager;

    @PostPersist
    @PostUpdate
    @PostRemove
    void onChange(Object entity) {
        switch (entity) {
            case User user -> afterCompletion(cache -> cache.evict(user.getId()));
            case UserPermission permission -> afterCompletion(cache -> cache.evict(permission.getUser().getId()));
            case UserCredential credential -> afterCompletion(cache -> cache.evict(credential.getUser().getId()));
            // ponytail: 부서 권한 프리셋이 바뀌면 전체를 비운다. 사용자가 수천 명이 되면 그 부서 소속만 지운다
            case DepartmentPermission ignored -> afterCompletion(Cache::clear);
            default -> { }
        }
    }

    // flush 시점에 지우면 커밋 전에 들어온 요청이 옛 값을 다시 넣는다. 커밋(또는 롤백) 뒤에 지운다
    private void afterCompletion(Consumer<Cache> action) {
        Cache cache = cacheManager.getCache(CACHE);
        if (cache == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.accept(cache);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                action.accept(cache);
            }
        });
    }
}
