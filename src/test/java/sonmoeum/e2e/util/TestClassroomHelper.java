package sonmoeum.e2e.util;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sonmoeum.domain.classroom.entity.Classroom;
import sonmoeum.domain.classroom.enums.ClassroomType;
import sonmoeum.domain.classroom.repository.ClassroomRepository;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class TestClassroomHelper {
    private static final Logger log = LoggerFactory.getLogger(TestClassroomHelper.class);
    private final ClassroomRepository classroomRepository;
    private final Map<Long, Classroom> classroomCache;

    public TestClassroomHelper(
            ClassroomRepository classroomRepository
    ) {
        this.classroomRepository = classroomRepository;
        this.classroomCache = new HashMap<>();
    }

    public Classroom createTestClassroom(String name, ClassroomType type) {
        Classroom saved = classroomRepository.save(
                Classroom.builder().name(name).type(type).build()
        );
        classroomCache.put(saved.getId(), saved);
        return saved;
    }

    public  void registerClassroom(Long classId) {
        classroomRepository.findById(classId).ifPresentOrElse(
                classroom -> classroomCache.put(classId, classroom),
                () -> {
                    log.warn("분반(ID: {})를 찾을 수 없음.", classId);
                }
        );
    }

    public void clearAll() {
        if (!classroomCache.isEmpty()) {
            log.info("테스트 분반 정리 중... 총 {}개 분반 삭제", classroomCache.size());
            classroomRepository.deleteAll(classroomCache.values());
            classroomRepository.flush();  // 즉시 DB에 반영
            classroomCache.clear();
        }
    }
}
