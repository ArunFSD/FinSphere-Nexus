package com.finsphere.auth;

import com.finsphere.auth.repository.UserRepository;
import com.finsphere.auth.repository.UserSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // New Import

@SpringBootTest
class AuthServiceApplicationTests {

	// Replacing the deprecated @MockBean with the new @MockitoBean
	@MockitoBean
	private UserSessionRepository userSessionRepository;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	void contextLoads() {
		// This ensures the Spring Context initializes correctly
		// without needing a real Redis or Postgres running.
	}
}