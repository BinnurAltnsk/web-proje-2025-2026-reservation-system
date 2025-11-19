package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.PaymentRequestDTO;
import com.teamreserve.reservationsystem.dto.PaymentResponseDTO;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(
        name = "Payment Controller",
        description = "Rezervasyon ödemelerini ve iade işlemlerini yöneten controller."
)
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String getUserEmail(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUsernameFromJWT(token);
        }
        throw new RuntimeException("User not authenticated");
    }

    @PostMapping("/process")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "Ödeme işlemini tamamla")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDTO request, HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            return ResponseEntity.ok(paymentService.processPayment(request, email));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Rezervasyon için ödeme bilgilerini getir")
    public ResponseEntity<?> getPaymentByReservation(@PathVariable Long reservationId, HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            return ResponseEntity.ok(paymentService.getPaymentByReservation(reservationId, email, isAdmin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "Kullanıcının yaptığı ödemeleri listele")
    public List<PaymentResponseDTO> getMyPayments(HttpServletRequest request) {
        String email = getUserEmail(request);
        return paymentService.getMyPayments(email);
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Ödeme iadesi işlemi")
    public ResponseEntity<?> refundPayment(@PathVariable Long paymentId, HttpServletRequest request) {
        try {
            PaymentResponseDTO response = paymentService.refundPayment(paymentId, request.isUserInRole("ROLE_ADMIN"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
