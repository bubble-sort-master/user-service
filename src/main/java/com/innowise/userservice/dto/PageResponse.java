package com.innowise.userservice.dto;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        boolean empty,
        boolean first,
        boolean last,
        int number,
        int numberOfElements,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getNumberOfElements(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}