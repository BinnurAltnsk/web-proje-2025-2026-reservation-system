package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.model.UserProfile;
import com.teamreserve.reservationsystem.repository.UserProfileRepository;
import com.teamreserve.reservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.regex.Pattern;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,11}$");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{16}$");

    /**
     * Get user profile with address and card info
     */
    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(String email) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(null);

        return buildProfileResponse(user, profile);
    }

    /**
     * Update user profile (name, email, phone, address, card)
     */
    @Transactional
    public ProfileResponseDTO updateProfile(String currentEmail, ProfileRequestDTO request) {
        ApplicationUser user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic user info
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            validateName(request.getName());
            user.setFullName(request.getName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(currentEmail)) {
                validateEmail(newEmail);
                // Check if email already exists
                if (userRepository.findByEmail(newEmail).isPresent()) {
                    throw new RuntimeException("Email already exists");
                }
                user.setEmail(newEmail);
            }
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            validatePhone(request.getPhone());
            user.setPhone(request.getPhone().trim());
        }

        userRepository.save(user);

        // Update or create profile for address and card
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile());
        
        if (profile.getUser() == null) {
            profile.setUser(user);
        }

        // Update address if provided
        if (request.getAddress() != null) {
            AddressDTO address = request.getAddress();
            if (address.getStreet() != null) profile.setStreet(address.getStreet().trim());
            if (address.getCity() != null) profile.setCity(address.getCity().trim());
            if (address.getState() != null) profile.setState(address.getState().trim());
            if (address.getZipCode() != null) profile.setZipCode(address.getZipCode().trim());
            if (address.getCountry() != null) profile.setCountry(address.getCountry().trim());
        }

        // Update card if provided
        if (request.getCard() != null) {
            CardDTO card = request.getCard();
            
            if (card.getCardNumber() != null && !card.getCardNumber().trim().isEmpty()) {
                String cardNumber = card.getCardNumber().replaceAll("\\s+", "");
                validateCardNumber(cardNumber);
                profile.setCardNumberMasked(maskCardNumber(cardNumber));
                // In production, you would tokenize the card here via payment gateway
                // profile.setCardToken(paymentGateway.tokenize(cardNumber));
            }
            
            if (card.getCardHolder() != null && !card.getCardHolder().trim().isEmpty()) {
                profile.setCardHolder(card.getCardHolder().trim().toUpperCase());
            }
            
            if (card.getExpiryMonth() != null && !card.getExpiryMonth().trim().isEmpty()) {
                validateExpiryMonth(card.getExpiryMonth());
                profile.setExpiryMonth(card.getExpiryMonth().trim());
            }
            
            if (card.getExpiryYear() != null && !card.getExpiryYear().trim().isEmpty()) {
                validateExpiryYear(card.getExpiryYear());
                profile.setExpiryYear(card.getExpiryYear().trim());
            }
        }

        userProfileRepository.save(profile);

        return buildProfileResponse(user, profile);
    }

    /**
     * Update password
     */
    @Transactional
    public void updatePassword(String email, UpdatePasswordDTO request) {
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
            throw new RuntimeException("Current password is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        // Check if new password is different from current
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Hash and save new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Update email
     */
    @Transactional
    public ProfileResponseDTO updateEmail(String currentEmail, String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        ApplicationUser user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String email = newEmail.trim().toLowerCase();
        validateEmail(email);

        if (!email.equals(currentEmail)) {
            // Check if email already exists
            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email);
            userRepository.save(user);
        }

        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        return buildProfileResponse(user, profile);
    }

    /**
     * Update phone
     */
    @Transactional
    public ProfileResponseDTO updatePhone(String email, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Phone number is required");
        }

        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validatePhone(phone);
        user.setPhone(phone.trim());
        userRepository.save(user);

        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        return buildProfileResponse(user, profile);
    }

    /**
     * Build profile response DTO
     */
    private ProfileResponseDTO buildProfileResponse(ApplicationUser user, UserProfile profile) {
        ProfileResponseDTO response = new ProfileResponseDTO();
        response.setId(user.getId());
        response.setName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getUserRole().name().replace("ROLE_", "").toLowerCase());

        // Add address if profile exists
        if (profile != null) {
            if (hasAddress(profile)) {
                AddressDTO address = new AddressDTO();
                address.setStreet(profile.getStreet());
                address.setCity(profile.getCity());
                address.setState(profile.getState());
                address.setZipCode(profile.getZipCode());
                address.setCountry(profile.getCountry());
                response.setAddress(address);
            }

            // Add masked card info if exists
            if (hasCard(profile)) {
                CardDTO card = new CardDTO();
                card.setCardNumber(profile.getCardNumberMasked()); // Already masked
                card.setCardHolder(profile.getCardHolder());
                card.setExpiryMonth(profile.getExpiryMonth());
                card.setExpiryYear(profile.getExpiryYear());
                response.setCard(card);
            }
        }

        return response;
    }

    /**
     * Check if profile has address data
     */
    private boolean hasAddress(UserProfile profile) {
        return StringUtils.hasText(profile.getStreet()) ||
               StringUtils.hasText(profile.getCity()) ||
               StringUtils.hasText(profile.getState()) ||
               StringUtils.hasText(profile.getZipCode()) ||
               StringUtils.hasText(profile.getCountry());
    }

    /**
     * Check if profile has card data
     */
    private boolean hasCard(UserProfile profile) {
        return StringUtils.hasText(profile.getCardNumberMasked()) ||
               StringUtils.hasText(profile.getCardHolder()) ||
               StringUtils.hasText(profile.getExpiryMonth()) ||
               StringUtils.hasText(profile.getExpiryYear());
    }

    /**
     * Mask card number - show only last 4 digits
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    // ========== Validation Methods ==========

    private void validateName(String name) {
        if (name == null || name.trim().length() < 3) {
            throw new RuntimeException("Name must be at least 3 characters");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Invalid email format");
        }
    }

    private void validatePhone(String phone) {
        String cleanPhone = phone.replaceAll("\\s+", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new RuntimeException("Phone number must be 10-11 digits");
        }
    }

    private void validateCardNumber(String cardNumber) {
        if (!CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            throw new RuntimeException("Card number must be 16 digits");
        }
    }

    private void validateExpiryMonth(String month) {
        try {
            int m = Integer.parseInt(month);
            if (m < 1 || m > 12) {
                throw new RuntimeException("Expiry month must be between 01 and 12");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid expiry month format");
        }
    }

    private void validateExpiryYear(String year) {
        try {
            int y = Integer.parseInt(year);
            int currentYear = Year.now().getValue();
            if (y < currentYear) {
                throw new RuntimeException("Expiry year cannot be in the past");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid expiry year format");
        }
    }
}

