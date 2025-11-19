package com.teamreserve.reservationsystem.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private Long reservationId;
    private String paymentMethod;
    private PaymentRequestDTO.CardDetails cardDetails;

    @Data
    public static class CardDetails {
        private String cardNumber;
        private String cardHolder;
        private String expiryMonth;
        private String expiryYear;
        private String cvv;
    }
}