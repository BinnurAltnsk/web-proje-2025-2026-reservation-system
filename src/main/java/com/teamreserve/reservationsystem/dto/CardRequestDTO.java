package com.teamreserve.reservationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestDTO {
    private String cardNumber;    // Full 16 digits (used only to generate masked + token, then discarded)
    private String cardHolder;
    private String expiryMonth;   // "01" to "12"
    private String expiryYear;    // "2027"
    private String cvv;           // 3 digits - NEVER stored, only validated
}

