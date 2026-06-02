package com.meeny.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// 페이지네이션 응답 공통 포맷. Spring Data 의 Page<T> 를 그대로 노출하지 않기 위한 wrapper.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(
                content.stream().map(mapper).toList(),
                page,
                size,
                totalElements,
                totalPages,
                last
        );
    }
}
