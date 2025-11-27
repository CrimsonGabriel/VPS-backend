package com.bazunia.vps.controller;

import com.bazunia.vps.dto.LoginRequest;
import com.bazunia.vps.service.AuthService;
import com.bazunia.vps.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;


    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_ShouldReturnOk_WhenServiceReturnsResponse() throws Exception {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest("admin@bazunia.pl", "admin123");

        AuthService.AuthResponse mockResponse = new AuthService.AuthResponse(
                "fake-jwt-token",
                false,
                "Zalogowano",
                false
        );

        when(authService.loginUser(any(LoginRequest.class), any())).thenReturn(mockResponse);

        // ACT & ASSERT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("fake-jwt-token"))
                .andExpect(jsonPath("$.message").value("Zalogowano"));
    }
}