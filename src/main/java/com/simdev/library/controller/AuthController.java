package com.simdev.library.controller;

import com.simdev.library.domain.enums.UserRole;
import com.simdev.library.dto.*;
import com.simdev.library.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints de connexion et d'inscription")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;

    @PostMapping("/login")
    @Operation(summary = "Connexion avec identifiants", description = "Vérifie un couple identifiant/mot de passe et retourne les informations de l'utilisateur.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserAccountResponse account = userAccountService.findByUsername(authentication.getName());

        return ResponseEntity.ok(new LoginResponse(
                "Authentification réussie",
                account.username(),
                account.email(),
                account.role()
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un utilisateur", description = "Crée un compte utilisateur avec le rôle MEMBER par défaut.")
    public ResponseEntity<UserAccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccountRequest userAccountRequest = new UserAccountRequest(
                request.username(),
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                UserRole.MEMBER,
                true
        );

        UserAccountResponse response = userAccountService.createUser(userAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

