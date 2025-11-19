package com.teamreserve.reservationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequestDTO {
    private String name;
    private String email;
    private String phone;
    private AddressDTO address;
    private CardDTO card;
}

