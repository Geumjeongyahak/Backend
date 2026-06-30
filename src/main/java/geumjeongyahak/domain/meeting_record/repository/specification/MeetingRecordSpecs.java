package geumjeongyahak.domain.meeting_record.repository.specification;

import geumjeongyahak.domain.meeting_record.entity.MeetingRecord;
import geumjeongyahak.domain.meeting_record.enums.MeetingRecordStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class MeetingRecordSpecs {

    private MeetingRecordSpecs() {
    }

    public static Specification<MeetingRecord> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<MeetingRecord> containsTitle(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("title")), pattern);
        };
    }

    public static Specification<MeetingRecord> hasAuthorId(Long authorId) {
        return (root, query, cb) -> authorId == null ? null : cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<MeetingRecord> hasStatus(MeetingRecordStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
