package com.madhurgram.productservice.common.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Stable, production-grade API wrapper for all paginated responses.
 *
 * <p>Decouples the API contract from Spring Data's internal {@code PageImpl}
 * structure, which can change between Spring Data versions. This ensures that
 * the JSON response format is always predictable and never breaks due to
 * framework upgrades.
 *
 * <p>Expected JSON shape:
 * <pre>
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 56,
 *   "totalPages": 3,
 *   "first": true,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> the type of content in the page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /**
     * Factory method to create a {@link PageResponse} from a Spring Data {@link Page}.
     *
     * @param springPage the Spring Data page result
     * @param <T>        the content type
     * @return a stable, serializable page response
     */
    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast()
        );
    }
}
