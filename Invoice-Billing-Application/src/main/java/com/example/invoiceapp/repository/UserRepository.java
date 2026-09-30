package com.example.invoiceapp.repository;

import com.example.invoiceapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    List<User> findByOrganizationId(Long organizationId);
}
