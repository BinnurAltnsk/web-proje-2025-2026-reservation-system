package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.CardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/profile/cards")
@Tag(name = "Card Controller")
public class CardController {

    @Autowired private CardService cardService;
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
    public ResponseEntity<?> getCards(HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            List<SavedCardDTO> cards = cardService.getCards(email);
            return ResponseEntity.ok(ApiResponse.success("Kayıtlı kartlar listelendi", cards));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Kartlar getirilemedi", "INTERNAL_ERROR", null));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> addCard(@RequestBody CardRequestDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            SavedCardDTO card = cardService.addCard(email, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Kart başarıyla eklendi", card));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Geçersiz kart bilgileri", "VALIDATION_ERROR", Collections.singletonList(e.getMessage())));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> deleteCard(@PathVariable Long id, HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            cardService.deleteCard(email, id);
            // 204 No Content
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("belong to user")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiErrorResponse.error("Bu işlem için yetkiniz yok", "FORBIDDEN", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Kart bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> setDefaultCard(@PathVariable Long id, HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            SavedCardDTO card = cardService.setDefaultCard(email, id);
            return ResponseEntity.ok(ApiResponse.success("Varsayılan kart güncellendi", card));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.error("Kart bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiErrorResponse.error("Erişim reddedildi", "FORBIDDEN", Collections.singletonList(e.getMessage())));
        }
    }
}