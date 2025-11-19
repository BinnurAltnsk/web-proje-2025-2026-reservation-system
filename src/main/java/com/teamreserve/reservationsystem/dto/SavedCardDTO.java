package com.teamreserve.reservationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedCardDTO {
    private Long id;
    private String cardNumber;    // Masked: "**** **** **** 1234"
    private String cardHolder;
    private String expiryMonth;
    private String expiryYear;
    private Boolean isDefault;
}

