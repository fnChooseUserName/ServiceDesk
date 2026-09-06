package com.servicedesk.ticketing.category.dto;

import com.servicedesk.ticketing.category.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        int defaultSlaHours
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDefaultSlaHours()
        );
    }
}
