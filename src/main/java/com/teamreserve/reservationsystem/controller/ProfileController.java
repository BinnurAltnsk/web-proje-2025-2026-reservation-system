package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@Tag(
        name = "Profile Controller",
        description = "Kullanıcı profil yönetimi - kişisel bilgiler, adres ve kart bilgileri"
)
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * Extract user email from JWT token in Authorization header
     */
    private String getUserEmail(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUsernameFromJWT(token);
        }
        throw new RuntimeException("User not authenticated");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(
            summary = "Kullanıcı profilini getir",
            description = "Giriş yapmış kullanıcının tam profil bilgilerini döner (ad, email, telefon, adres, kart bilgileri)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil bilgileri başarıyla getirildi",
                    content = @Content(schema = @Schema(implementation = ProfileResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim - JWT token geçersiz veya eksik",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Kullanıcı bulunamadı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            ProfileResponseDTO profile = profileService.getProfile(email);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(
            summary = "Profil bilgilerini güncelle",
            description = "Kullanıcının kişisel bilgilerini, adres ve kart bilgilerini günceller. Tüm alanlar opsiyonel, sadece gönderilen alanlar güncellenir."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil başarıyla güncellendi",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz veri veya email zaten kullanımda",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            ProfileResponseDTO updatedProfile = profileService.updateProfile(email, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", updatedProfile
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/password")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(
            summary = "Şifre güncelle",
            description = "Kullanıcının şifresini günceller. Mevcut şifre doğrulanır ve yeni şifre BCrypt ile hash'lenerek kaydedilir."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Şifre başarıyla güncellendi",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Mevcut şifre hatalı veya yeni şifre geçersiz",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> updatePassword(
            @RequestBody UpdatePasswordDTO request,
            HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            profileService.updatePassword(email, request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/email")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(
            summary = "Email güncelle",
            description = "Kullanıcının email adresini günceller. Yeni email benzersiz olmalıdır."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email başarıyla güncellendi",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz email formatı veya email zaten kullanımda",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> updateEmail(
            @RequestBody UpdateEmailDTO request,
            HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            ProfileResponseDTO updatedProfile = profileService.updateEmail(email, request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "Email updated successfully",
                    "user", updatedProfile
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/phone")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(
            summary = "Telefon güncelle",
            description = "Kullanıcının telefon numarasını günceller. 10-11 rakam olmalıdır."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Telefon başarıyla güncellendi",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz telefon formatı",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> updatePhone(
            @RequestBody UpdatePhoneDTO request,
            HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            ProfileResponseDTO updatedProfile = profileService.updatePhone(email, request.getPhone());
            return ResponseEntity.ok(Map.of(
                    "message", "Phone updated successfully",
                    "user", updatedProfile
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

