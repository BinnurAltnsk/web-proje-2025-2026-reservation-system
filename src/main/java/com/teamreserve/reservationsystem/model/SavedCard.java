package com.teamreserve.reservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private ApplicationUser user;

    @Column(name = "masked_number", nullable = false, length = 20)
    private String maskedNumber; // Format: "**** **** **** 1234"

    @Column(name = "card_holder", nullable = false, length = 100)
    private String cardHolder;

    @Column(name = "expiry_month", nullable = false, length = 2)
    private String expiryMonth; // Format: "01" to "12"

    @Column(name = "expiry_year", nullable = false, length = 4)
    private String expiryYear; // Format: "2027"

    @Column(name = "token", length = 500)
    private String token; // UUID or payment gateway token

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // IMPORTANT SECURITY NOTES:
    // 1. CVV is NEVER stored - only used during payment processing
    // 2. Full card number is NEVER stored - only masked version
    // 3. Token can be used for payment gateway integration
}

