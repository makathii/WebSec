package com.websec.websec.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, boolean hasNext) {

	public static <T> PageResponse<T> of(Page<?> page, List<T> items) {
		return new PageResponse<>(items, page.getNumber(), page.hasNext());
	}
}
