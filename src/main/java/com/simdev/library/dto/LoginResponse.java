package com.simdev.library.dto;

import com.simdev.library.domain.enums.UserRole;

public record LoginResponse(
        String message,
        String username,
        String email,
        UserRole role
) { }

