package com.simdev.library.controller;

import com.simdev.library.dto.UserAccountRequest;
import com.simdev.library.dto.UserAccountResponse;
import com.simdev.library.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des comptes utilisateurs et des rôles")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @GetMapping
    @Operation(summary = "Lister les utilisateurs")
    public ResponseEntity<Page<UserAccountResponse>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(userAccountService.getUsers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un utilisateur par ID")
    public ResponseEntity<UserAccountResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userAccountService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<UserAccountResponse> createUser(@Valid @RequestBody UserAccountRequest request) {
        UserAccountResponse response = userAccountService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un utilisateur")
    public ResponseEntity<UserAccountResponse> updateUser(@PathVariable Long id,
                                                          @Valid @RequestBody UserAccountRequest request) {
        return ResponseEntity.ok(userAccountService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un utilisateur")
    public void deleteUser(@PathVariable Long id) {
        userAccountService.deleteUser(id);
    }
}

