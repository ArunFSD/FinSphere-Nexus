package com.finsphere.chit.repository.jpa;

import java.util.Optional;

import com.finsphere.chit.domain.UserMirror;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMirrorRepository extends JpaRepository<UserMirror, Long> {

    Optional<UserMirror> findByUserIdAndActiveTrue(Long userId);

    Page<UserMirror> findByActiveTrue(Pageable pageable);
}
