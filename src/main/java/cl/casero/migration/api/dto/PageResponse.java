package cl.casero.migration.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content,
                              int page,
                              int size,
                              int totalPages,
                              long totalElements,
                              boolean hasPrevious,
                              boolean hasNext) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasPrevious(),
                page.hasNext());
    }
}
