package com.finsphere.common.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * A stable, Jackson-friendly page representation.
 * <p>
 * Spring Data's {@code PageImpl} has no stable JSON shape and cannot be deserialized by clients, so every
 * paginated endpoint returns this record instead.
 *
 * @param content       items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements total items across all pages
 * @param totalPages    total number of pages
 * @param first         whether this is the first page
 * @param last          whether this is the last page
 * @param <T>           item type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public PageResponse {
        content = (content != null) ? List.copyOf(content) : List.of();
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <S, T> PageResponse<T> from(Page<S> page, Function<? super S, ? extends T> mapper) {
        List<T> mapped = page.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(mapped, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <T> PageResponse<T> empty(int size) {
        return new PageResponse<>(List.of(), 0, size, 0, 0, true, true);
    }
}
