package com.teamreserve.reservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private ApplicationUser user;

    // Address fields
    @Column(length = 500)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String zipCode;

    @Column(length = 100)
    private String country;

    // Card fields (masked/tokenized for security)
    @Column(length = 20)
    private String cardNumberMasked; // Format: "**** **** **** 1234"

    @Column(length = 500)
    private String cardToken; // Optional: for payment gateway integration

    @Column(length = 100)
    private String cardHolder;

    @Column(length = 2)
    private String expiryMonth;

    @Column(length = 4)
    private String expiryYear;

    // NEVER store CVV or full card number in plain text
}

