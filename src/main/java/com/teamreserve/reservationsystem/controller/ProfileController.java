package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.ProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile Controller")
public class ProfileController {

    @Autowired private ProfileService profileService;
    @Autowired private JwtTokenProvider tokenProvider;

    private String getUserEmail(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return tokenProvider.getUsernameFromJWT(bearerToken.substring(7));
        }
        throw new RuntimeException("Kullanıcı kimliği doğrulanamadı");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            return ResponseEntity.ok(ApiResponse.success("Profil bilgileri getirildi", profileService.getProfile(email)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Profil bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileRequestDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            ProfileResponseDTO updatedProfile = profileService.updateProfile(email, request);
            return ResponseEntity.ok(ApiResponse.success("Profil başarıyla güncellendi", updatedProfile));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiErrorResponse.error("Bu e-posta zaten kullanımda", "CONFLICT", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Profil güncellenemedi", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/password")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            profileService.updatePassword(email, request);
            return ResponseEntity.ok(ApiResponse.success("Şifre başarıyla güncellendi"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Şifre güncelleme başarısız", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/email")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> updateEmail(@RequestBody UpdateEmailDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            ProfileResponseDTO updatedProfile = profileService.updateEmail(email, request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("E-posta adresi başarıyla güncellendi", updatedProfile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("E-posta güncellenemedi", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/phone")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> updatePhone(@RequestBody UpdatePhoneDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            ProfileResponseDTO updatedProfile = profileService.updatePhone(email, request.getPhone());
            return ResponseEntity.ok(ApiResponse.success("Telefon numarası başarıyla güncellendi", updatedProfile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Telefon güncellenemedi", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }
}