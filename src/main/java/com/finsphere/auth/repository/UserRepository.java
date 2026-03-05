package com.finsphere.auth.repository;

import com.finsphere.auth.entity.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // This allows one login field to check both database columns
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :identifier OR u.email = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}
