package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.ReservationService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservation Controller")
public class ReservationController {

    @Autowired private ReservationService reservationService;
    @Autowired private JwtTokenProvider tokenProvider;

    private String getUsernameFromToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return tokenProvider.getUsernameFromJWT(bearerToken.substring(7));
        }
        throw new RuntimeException("Kullanıcı kimliği doğrulanamadı");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequestDTO requestDTO, HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            ReservationResponseDTO reservation = reservationService.createReservation(requestDTO, email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Rezervasyon başarıyla oluşturuldu", reservation));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.error("İlgili kaynak bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Rezervasyon oluşturulamadı", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @GetMapping("/my-reservations")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> getMyReservations(HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            List<ReservationResponseDTO> reservations = reservationService.getMyReservations(email);
            return ResponseEntity.ok(ApiResponse.success("Rezervasyonlarınız listelendi", reservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Rezervasyonlar getirilirken hata oluştu", "INTERNAL_ERROR", null));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getReservationById(@PathVariable Long id, HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            ReservationResponseDTO dto = reservationService.getReservationForUser(id, email, isAdmin);
            return ResponseEntity.ok(ApiResponse.success("Rezervasyon detayları getirildi", dto));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not allowed") || e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiErrorResponse.error("Erişim reddedildi", "FORBIDDEN", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Rezervasyon bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id, HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            reservationService.cancelReservation(id, email, isAdmin);
            return ResponseEntity.ok(ApiResponse.success("Rezervasyon başarıyla iptal edildi", Map.of("refundAmount", 0)));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("only cancel your own")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiErrorResponse.error("Sadece kendi rezervasyonunuzu iptal edebilirsiniz", "FORBIDDEN", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Rezervasyon bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long salonId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Rezervasyonlar listelendi", reservationService.getAllReservations(status, date, salonId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiErrorResponse.error("Hata oluştu", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getCalendarData() {
        try {
            return ResponseEntity.ok(ApiResponse.success("Takvim verileri getirildi", reservationService.getCalendarReservations()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Takvim verisi alınamadı", "INTERNAL_ERROR", null));
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getPendingReservations() {
        return ResponseEntity.ok(ApiResponse.success("Bekleyen rezervasyonlar getirildi", reservationService.getPendingReservations()));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getUpcomingReservations() {
        return ResponseEntity.ok(ApiResponse.success("Yaklaşan rezervasyonlar getirildi", reservationService.getUpcomingReservations()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> approveReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Rezervasyon onaylandı", reservationService.approveReservation(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Rezervasyon bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> rejectReservation(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Rezervasyon reddedildi", reservationService.rejectReservation(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Rezervasyon bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateReservationStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (!StringUtils.hasText(status)) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Durum (status) alanı zorunludur", "BAD_REQUEST", null));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success("Durum güncellendi", reservationService.updateReservationStatus(id, status)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Durum güncellenemedi", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }
}