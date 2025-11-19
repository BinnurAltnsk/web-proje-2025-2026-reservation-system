package com.teamreserve.reservationsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private AddressDTO address;
    private CardDTO card; // Card info will be masked when returned
}

