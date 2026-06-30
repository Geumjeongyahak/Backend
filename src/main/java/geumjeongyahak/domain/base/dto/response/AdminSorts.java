package geumjeongyahak.domain.base.dto.response;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdminSorts {

    private AdminSorts() {
    }

    public static <T> List<T> sort(List<T> rows, String sort, Map<String, Comparator<T>> comparators, String defaultSort) {
        Comparator<T> comparator = buildComparator(sort, comparators);
        if (comparator == null) {
            comparator = buildComparator(defaultSort, comparators);
        }
        if (comparator == null) {
            return rows;
        }
        return rows.stream().sorted(comparator).toList();
    }

    private static <T> Comparator<T> buildComparator(String sort, Map<String, Comparator<T>> comparators) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        Comparator<T> result = null;
        String[] clauses = sort.split(";");
        for (String clause : clauses) {
            if (clause == null || clause.isBlank()) {
                continue;
            }
            String[] parts = clause.split(",");
            if (parts.length < 1) {
                continue;
            }
            String field = parts[0].trim();
            Comparator<T> fieldComparator = comparators.get(field);
            if (fieldComparator == null) {
                continue;
            }
            String direction = parts.length > 1 ? parts[1].trim().toUpperCase(Locale.ROOT) : "ASC";
            if ("DESC".equals(direction)) {
                fieldComparator = fieldComparator.reversed();
            }
            result = result == null ? fieldComparator : result.thenComparing(fieldComparator);
        }
        return result;
    }
}
