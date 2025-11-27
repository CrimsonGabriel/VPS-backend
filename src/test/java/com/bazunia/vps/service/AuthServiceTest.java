package com.bazunia.vps.service;

import com.bazunia.vps.dto.LoginRequest;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StatusService statusService;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TwoFactorService twoFactorService;

    @InjectMocks private AuthService authService;

    @Test
    void loginUser_ShouldReturnToken_WhenCredentialsAreValid() {
        // ARRANGE
        String email = "test@bazunia.pl";
        String password = "password123";
        String ip = "127.0.0.1";
        String expectedToken = "jwt-token-xyz";

        LoginRequest request = new LoginRequest(email, password);

        User mockUser = new User();
        mockUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(twoFactorService.is2FaRequired(mockUser)).thenReturn(false);
        when(jwtService.generateToken(mockUser)).thenReturn(expectedToken);

        // ACT
        AuthService.AuthResponse response = authService.loginUser(request, ip);

        // ASSERT
        assertNotNull(response);
        assertEquals(expectedToken, response.jwt());
        assertFalse(response.requires2FA());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(statusService).addAndroidIp(ip);
    }

    @Test
    void loginUser_ShouldThrowException_WhenUserNotFound() {
        // ARRANGE
        LoginRequest request = new LoginRequest("nieznany@bazunia.pl", "haslo");


        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> authService.loginUser(request, "127.0.0.1"));
    }
}