package com.finsphere.repository;

import com.finsphere.entity.UserMirror;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMirrorRepository extends JpaRepository<UserMirror, Long> {

}
