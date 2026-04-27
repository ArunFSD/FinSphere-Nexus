package com.finsphere.repository.jpa;

import com.finsphere.entity.UserMirror;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMirrorRepository extends JpaRepository<UserMirror, Long> {

    Optional<UserMirror> findByPhoneNumber(String phoneNumber);

}
