package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.PaymentService;
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
@RequestMapping("/api/payments")
@Tag(name = "Payment Controller")
public class PaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private JwtTokenProvider tokenProvider;

    private String getUserEmail(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return tokenProvider.getUsernameFromJWT(bearerToken.substring(7));
        }
        throw new RuntimeException("Kullanıcı kimliği doğrulanamadı");
    }

    @PostMapping("/process")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            PaymentResponseDTO response = paymentService.processPayment(request, email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Ödeme işlemi başarıyla tamamlandı", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.error("Kaynak bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Ödeme işlemi başarısız", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getPaymentByReservation(@PathVariable Long reservationId, HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            return ResponseEntity.ok(ApiResponse.success("Ödeme detayları getirildi",
                    paymentService.getPaymentByReservation(reservationId, email, isAdmin)));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not allowed")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiErrorResponse.error("Erişim reddedildi", "FORBIDDEN", Collections.singletonList(e.getMessage())));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Ödeme bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<?> getMyPayments(HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            return ResponseEntity.ok(ApiResponse.success("Ödemeleriniz listelendi", paymentService.getMyPayments(email)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Ödemeler getirilemedi", "INTERNAL_ERROR", null));
        }
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> refundPayment(@PathVariable Long paymentId, HttpServletRequest request) {
        try {
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            PaymentResponseDTO response = paymentService.refundPayment(paymentId, isAdmin);
            return ResponseEntity.ok(ApiResponse.success("İade işlemi başarılı", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("İade işlemi başarısız", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }
}