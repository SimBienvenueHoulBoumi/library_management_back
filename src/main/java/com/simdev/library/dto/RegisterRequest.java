package com.simdev.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères.")
        String username,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 80, message = "Le prénom ne peut pas dépasser 80 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 80, message = "Le nom ne peut pas dépasser 80 caractères.")
        String lastName,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "Adresse email invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères.")
        String password
) { }

