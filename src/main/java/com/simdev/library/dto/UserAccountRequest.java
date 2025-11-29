package com.simdev.library.dto;

import com.simdev.library.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserAccountRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(max = 50, message = "Le nom d'utilisateur ne peut pas dépasser 50 caractères.")
        String username,

        @Email(message = "Adresse email invalide.")
        @NotBlank(message = "L'email est obligatoire.")
        @Size(max = 120, message = "L'email ne peut pas dépasser 120 caractères.")
        String email,

        @Size(max = 255, message = "Le mot de passe est trop long.")
        String password,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 80, message = "Le prénom ne peut pas dépasser 80 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 80, message = "Le nom ne peut pas dépasser 80 caractères.")
        String lastName,

        UserRole role,

        Boolean enabled
) { }

