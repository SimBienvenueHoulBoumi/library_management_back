package com.simdev.library.service;

import com.simdev.library.dto.UserAccountRequest;
import com.simdev.library.dto.UserAccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAccountService {

    Page<UserAccountResponse> getUsers(Pageable pageable);

    UserAccountResponse getUser(Long id);

    UserAccountResponse createUser(UserAccountRequest request);

    UserAccountResponse updateUser(Long id, UserAccountRequest request);

    void deleteUser(Long id);

    UserAccountResponse findByUsername(String username);
}

