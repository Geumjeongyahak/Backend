package geumjeongyahak.domain.base.dto.response;

import java.util.List;

public record AdminPage<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> AdminPage<T> from(List<T> rows, Integer requestedPage, Integer requestedSize) {
        int safeSize = requestedSize == null || requestedSize <= 0 ? 10 : Math.min(requestedSize, 100);
        int safePage = requestedPage == null || requestedPage < 0 ? 0 : requestedPage;
        int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / safeSize);
        int fromIndex = Math.min(safePage * safeSize, rows.size());
        int toIndex = Math.min(fromIndex + safeSize, rows.size());

        return new AdminPage<>(
            rows.subList(fromIndex, toIndex),
            safePage,
            safeSize,
            rows.size(),
            totalPages
        );
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public int previousPage() {
        return Math.max(page - 1, 0);
    }

    public int nextPage() {
        return page + 1;
    }
}
