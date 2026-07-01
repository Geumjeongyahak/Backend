package geumjeongyahak.domain.purchase_request.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.enums.PurchaseRequestStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class PurchaseRequestSpecs {

    private PurchaseRequestSpecs() {
    }

    public static Specification<PurchaseRequest> hasStatus(PurchaseRequestStatus status) {
        return (root, query, cb) -> status == null
            ? cb.conjunction()
            : cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseRequest> requestedBy(Long requesterId) {
        return (root, query, cb) -> requesterId == null
            ? cb.conjunction()
            : cb.equal(root.get("requestedBy").get("id"), requesterId);
    }

    public static Specification<PurchaseRequest> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (isBlank(keyword)) {
                return cb.conjunction();
            }

            String pattern = containsPattern(keyword);
            Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
            Predicate classroomNameLike = cb.like(
                cb.lower(root.join("classroom", JoinType.LEFT).get("name")),
                pattern
            );
            Predicate requestedByNameLike = cb.like(
                cb.lower(root.join("requestedBy", JoinType.LEFT).get("name")),
                pattern
            );

            return cb.or(titleLike, classroomNameLike, requestedByNameLike);
        };
    }

    public static Specification<PurchaseRequest> classroomNameContains(String classroomName) {
        return (root, query, cb) -> isBlank(classroomName)
            ? cb.conjunction()
            : cb.like(cb.lower(root.join("classroom", JoinType.LEFT).get("name")), containsPattern(classroomName));
    }

    public static Specification<PurchaseRequest> requestedByNameContains(String requestedByName) {
        return (root, query, cb) -> isBlank(requestedByName)
            ? cb.conjunction()
            : cb.like(cb.lower(root.join("requestedBy", JoinType.LEFT).get("name")), containsPattern(requestedByName));
    }

    private static String containsPattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
