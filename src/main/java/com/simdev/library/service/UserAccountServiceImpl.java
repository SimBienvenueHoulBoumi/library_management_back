package com.simdev.library.service;

import com.simdev.library.domain.enums.UserRole;
import com.simdev.library.domain.model.UserAccount;
import com.simdev.library.dto.UserAccountRequest;
import com.simdev.library.dto.UserAccountResponse;
import com.simdev.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserAccountResponse> getUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountResponse getUser(Long id) {
        UserAccount userAccount = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id " + id + ")."));
        return toResponse(userAccount);
    }

    @Override
    public UserAccountResponse createUser(UserAccountRequest request) {
        validateUsernameAndEmail(request.username(), request.email(), null);

        UserAccount userAccount = UserAccount.builder()
                .username(request.username().trim())
                .email(request.email().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .password(passwordEncoder.encode(requirePassword(request.password())))
                .role(request.role() != null ? request.role() : UserRole.LIBRARIAN)
                .enabled(request.enabled() == null || request.enabled())
                .build();

        return toResponse(userAccountRepository.save(userAccount));
    }

    @Override
    public UserAccountResponse updateUser(Long id, UserAccountRequest request) {
        UserAccount existing = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id " + id + ")."));

        validateUsernameAndEmail(request.username(), request.email(), id);

        existing.setUsername(request.username().trim());
        existing.setEmail(request.email().trim());
        existing.setFirstName(request.firstName().trim());
        existing.setLastName(request.lastName().trim());
        if (StringUtils.hasText(request.password())) {
            existing.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            existing.setRole(request.role());
        }
        if (request.enabled() != null) {
            existing.setEnabled(request.enabled());
        }

        return toResponse(userAccountRepository.save(existing));
    }

    @Override
    public void deleteUser(Long id) {
        if (!userAccountRepository.existsById(id)) {
            throw new IllegalArgumentException("Utilisateur introuvable (id " + id + ").");
        }
        userAccountRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountResponse findByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (username " + username + ")."));
    }

    private String requirePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        return password;
    }

    private void validateUsernameAndEmail(String username, String email, Long currentUserId) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Le nom d'utilisateur et l'email sont obligatoires.");
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim();

        userAccountRepository.findByUsername(normalizedUsername)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Le nom d'utilisateur est déjà utilisé.");
                });

        userAccountRepository.findByEmail(normalizedEmail)
                .filter(existing -> currentUserId == null || !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("L'email est déjà utilisé.");
                });
    }

    private UserAccountResponse toResponse(UserAccount userAccount) {
        return new UserAccountResponse(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getEmail(),
                userAccount.getFirstName(),
                userAccount.getLastName(),
                userAccount.getRole(),
                userAccount.isEnabled(),
                userAccount.getCreatedDate(),
                userAccount.getLastModifiedDate()
        );
    }
}

