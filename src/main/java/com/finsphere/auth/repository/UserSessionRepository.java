package com.finsphere.auth.repository;

import com.finsphere.auth.model.UserSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSessionRepository extends CrudRepository<UserSession, String> {

    // Industrial search: Find all active sessions for a specific phone number
    // (Used for "Logout from all devices" feature)
    List<UserSession> findByPhoneNumber(String phoneNumber);
}