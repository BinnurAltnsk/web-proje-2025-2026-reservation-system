package com.teamreserve.reservationsystem.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String name;
    private String email;
    private String password;
    private String phone;
}