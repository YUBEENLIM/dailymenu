package com.example.dailymenu.user.adapter.in.web.dto;

import java.util.List;

public record UpdatePreferencesRequest(
        boolean preferSolo,
        Integer minPrice,
        Integer maxPrice,
        List<String> preferredCategories
) {}
