package com.simdev.library.dto;

import com.simdev.library.domain.enums.UserRole;

import java.time.LocalDateTime;

public record UserAccountResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean enabled,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) { }

