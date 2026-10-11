package com.finsphere.auth.repository;

import java.util.Optional;

import com.finsphere.auth.domain.UserAccount;
import com.finsphere.auth.domain.UserRole;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * Finds an account by phone number or (case-insensitive) e-mail, with its profile in the same query.
     * Backed by the unique index on {@code phone_number} and the functional index on {@code lower(email)}.
     */
    @EntityGraph(attributePaths = "profile")
    @Query("select u from UserAccount u where u.phoneNumber = :identifier or lower(u.email) = lower(:identifier)")
    Optional<UserAccount> findByIdentifier(@Param("identifier") String identifier);

    @EntityGraph(attributePaths = "profile")
    Optional<UserAccount> findWithProfileById(Long id);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(UserRole role);
}
