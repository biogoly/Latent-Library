package com.nilsson.backend.model;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(List<T> content, int totalPages, long totalElements, int number, int size, boolean last) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getTotalPages(), page.getTotalElements(),
                page.getNumber(), page.getSize(), page.isLast());
    }
}
