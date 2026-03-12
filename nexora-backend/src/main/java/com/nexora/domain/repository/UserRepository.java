package com.nexora.domain.repository;

import com.nexora.domain.model.User;
import com.nexora.domain.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port — interface que o domínio exige para persistência de usuários.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findAllByRole(UserRole role);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}