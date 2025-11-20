package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Controller")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private AuthService authService;
    @Autowired private JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail().trim().toLowerCase(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            ApplicationUser user = (ApplicationUser) authentication.getPrincipal();

            AuthResponseDTO authResponse = new AuthResponseDTO(jwt, toUserResponse(user));

            return ResponseEntity.ok(ApiResponse.success("Giriş başarılı", authResponse));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.error("E-posta veya şifre hatalı", "UNAUTHORIZED", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Giriş işlemi başarısız", "INTERNAL_ERROR", Collections.singletonList(e.getMessage())));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            ApplicationUser user = authService.registerUser(registerRequest);
            // 201 Created
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Kullanıcı başarıyla kaydedildi", toUserResponse(user)));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("already registered")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiErrorResponse.error("Kayıt başarısız: Bu e-posta zaten kullanımda", "EMAIL_EXISTS", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Kayıt başarısız", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Bir hata oluştu", "INTERNAL_ERROR", Collections.singletonList(e.getMessage())));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Başarıyla çıkış yapıldı"));
    }

    private UserResponseDTO toUserResponse(ApplicationUser user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getUserRole().name().replace("ROLE_", "").toLowerCase());
        return dto;
    }
}