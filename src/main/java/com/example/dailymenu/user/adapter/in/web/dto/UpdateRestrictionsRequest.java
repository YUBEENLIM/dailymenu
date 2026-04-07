package com.example.dailymenu.user.adapter.in.web.dto;

import java.util.List;

public record UpdateRestrictionsRequest(
        List<String> excludedCategories
) {}
