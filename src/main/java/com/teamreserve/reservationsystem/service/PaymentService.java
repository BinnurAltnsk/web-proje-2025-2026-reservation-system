package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.PaymentRequestDTO;
import com.teamreserve.reservationsystem.dto.PaymentResponseDTO;
import com.teamreserve.reservationsystem.model.Payment;
import com.teamreserve.reservationsystem.model.PaymentStatus;
import com.teamreserve.reservationsystem.model.Reservation;
import com.teamreserve.reservationsystem.model.SavedCard;
import com.teamreserve.reservationsystem.repository.PaymentRepository;
import com.teamreserve.reservationsystem.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CardService cardService;

    private PaymentResponseDTO toDto(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservation().getId())
                .status(payment.getStatus().name().toLowerCase())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .refundedAt(payment.getRefundedAt())
                .build();
    }

    private String extractCardLast4(PaymentRequestDTO.CardDetails details) {
        if (details == null || !StringUtils.hasText(details.getCardNumber())) {
            return null;
        }
        String digitsOnly = details.getCardNumber().replaceAll("\\s+", "");
        if (digitsOnly.length() < 4) {
            return digitsOnly;
        }
        return digitsOnly.substring(digitsOnly.length() - 4);
    }

    public PaymentResponseDTO processPayment(PaymentRequestDTO request, String requesterEmail) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!reservation.getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new RuntimeException("You can only pay for your own reservations");
        }

        if (reservation.getStatus() == com.teamreserve.reservationsystem.model.ReservationStatus.CANCELLED) {
            throw new RuntimeException("Cancelled reservations cannot be paid");
        }

        paymentRepository.findByReservationId(reservation.getId())
                .ifPresent(payment -> {
                    throw new RuntimeException("Payment already exists for this reservation");
                });

        Payment payment = new Payment();
        double amount = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : 0d;
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(UUID.randomUUID().toString());

        // Handle saved card payment
        if (request.getSavedCardId() != null) {
            // Payment with saved card
            if (!StringUtils.hasText(request.getCvv())) {
                throw new RuntimeException("CVV is required for saved card payment");
            }
            
            // Validate CVV format (3 digits)
            if (!request.getCvv().matches("^[0-9]{3}$")) {
                throw new RuntimeException("CVV must be 3 digits");
            }

            // Get the saved card (this also verifies it belongs to the user)
            SavedCard savedCard = cardService.getCardById(requesterEmail, request.getSavedCardId());
            
            // Extract last 4 digits from masked number
            String maskedNumber = savedCard.getMaskedNumber();
            if (maskedNumber != null && maskedNumber.length() >= 4) {
                payment.setCardLast4(maskedNumber.substring(maskedNumber.length() - 4));
            }

            // In production: Use savedCard.getToken() + CVV to process payment via payment gateway
            // payment.setTransactionId(paymentGateway.charge(savedCard.getToken(), request.getCvv(), amount));
        } else {
            // Normal payment with card details
            payment.setCardLast4(extractCardLast4(request.getCardDetails()));
        }

        return toDto(paymentRepository.save(payment));
    }

    public PaymentResponseDTO getPaymentByReservation(Long reservationId, String requesterEmail, boolean isAdmin) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException("Payment not found for reservation"));

        if (!isAdmin && !payment.getReservation().getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new RuntimeException("You are not allowed to view this payment");
        }

        return toDto(payment);
    }

    public List<PaymentResponseDTO> getMyPayments(String requesterEmail) {
        return paymentRepository.findByReservation_User_EmailOrderByCreatedAtDesc(requesterEmail).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PaymentResponseDTO refundPayment(Long paymentId, boolean isAdmin) {
        if (!isAdmin) {
            throw new RuntimeException("Only admins can refund payments");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new RuntimeException("Payment already refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        return toDto(paymentRepository.save(payment));
    }
}

