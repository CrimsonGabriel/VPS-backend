package com.bazunia.vps.controller;

import com.bazunia.vps.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    // Endpoint /auth/google
    @PostMapping("/auth/google")
    public ResponseEntity<AuthService.AuthResponse> authGoogle(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String token = body.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().build();
        }

        String ip = request.getRemoteAddr(); // Pobranie IP
        AuthService.AuthResponse response = authService.loginWithGoogle(token, ip);

        return ResponseEntity.ok(response);
    }

    // TODO: Dodać resztę endpointów 2FA (/2fa/setup, /2fa/verify, etc.)
    // Każdy z nich będzie wywoływał odpowiednią metodę w Twoim (jeszcze nie napisanym)
    // serwisie TwoFactorAuthService.
}