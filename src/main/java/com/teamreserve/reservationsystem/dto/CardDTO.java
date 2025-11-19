package com.teamreserve.reservationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {
    private String cardNumber;    // For save: full number (16 digits), for display: masked
    private String cardHolder;
    private String expiryMonth;   // 01-12
    private String expiryYear;    // Current year or future
    // CVV is NEVER stored, only used during payment processing
}

