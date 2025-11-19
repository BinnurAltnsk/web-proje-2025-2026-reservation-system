package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.CardRequestDTO;
import com.teamreserve.reservationsystem.dto.SavedCardDTO;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.model.SavedCard;
import com.teamreserve.reservationsystem.repository.SavedCardRepository;
import com.teamreserve.reservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CardService {

    @Autowired
    private SavedCardRepository savedCardRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{16}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3}$");

    /**
     * Get all saved cards for a user
     */
    @Transactional(readOnly = true)
    public List<SavedCardDTO> getCards(String userEmail) {
        ApplicationUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SavedCard> cards = savedCardRepository.findByUserId(user.getId());

        return cards.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Add a new card for a user
     */
    @Transactional
    public SavedCardDTO addCard(String userEmail, CardRequestDTO request) {
        // Validate input
        validateCardRequest(request);

        ApplicationUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if this is the first card
        long cardCount = savedCardRepository.countByUserId(user.getId());
        boolean isFirstCard = (cardCount == 0);

        // Create new saved card
        SavedCard card = new SavedCard();
        card.setUser(user);
        card.setMaskedNumber(maskCardNumber(request.getCardNumber()));
        card.setCardHolder(request.getCardHolder().trim().toUpperCase());
        card.setExpiryMonth(request.getExpiryMonth().trim());
        card.setExpiryYear(request.getExpiryYear().trim());
        card.setToken(generateToken(request.getCardNumber())); // Generate UUID token
        card.setIsDefault(isFirstCard); // First card is automatically default

        savedCardRepository.save(card);

        return toDTO(card);
    }

    /**
     * Delete a saved card
     */
    @Transactional
    public void deleteCard(String userEmail, Long cardId) {
        ApplicationUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavedCard card = savedCardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() -> new RuntimeException("Card not found or does not belong to user"));

        boolean wasDefault = card.getIsDefault();
        
        savedCardRepository.delete(card);

        // If the deleted card was default, set another card as default
        if (wasDefault) {
            List<SavedCard> remainingCards = savedCardRepository.findByUserId(user.getId());
            if (!remainingCards.isEmpty()) {
                SavedCard newDefaultCard = remainingCards.get(0);
                newDefaultCard.setIsDefault(true);
                savedCardRepository.save(newDefaultCard);
            }
        }
    }

    /**
     * Set a card as default
     */
    @Transactional
    public SavedCardDTO setDefaultCard(String userEmail, Long cardId) {
        ApplicationUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavedCard card = savedCardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() -> new RuntimeException("Card not found or does not belong to user"));

        // Unset all other cards as default
        List<SavedCard> userCards = savedCardRepository.findByUserId(user.getId());
        for (SavedCard c : userCards) {
            c.setIsDefault(false);
        }
        savedCardRepository.saveAll(userCards);

        // Set the selected card as default
        card.setIsDefault(true);
        savedCardRepository.save(card);

        return toDTO(card);
    }

    /**
     * Get a saved card by ID (for payment processing)
     */
    @Transactional(readOnly = true)
    public SavedCard getCardById(String userEmail, Long cardId) {
        ApplicationUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return savedCardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() -> new RuntimeException("Card not found or does not belong to user"));
    }

    // ========== Helper Methods ==========

    /**
     * Mask card number - show only last 4 digits
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String cleaned = cardNumber.replaceAll("\\s+", "");
        return "**** **** **** " + cleaned.substring(cleaned.length() - 4);
    }

    /**
     * Generate a unique token for the card (in production, use payment gateway tokenization)
     */
    private String generateToken(String cardNumber) {
        // In production, this should call a payment gateway to tokenize the card
        // For now, generate a UUID that includes a hash of the card number
        return UUID.randomUUID().toString();
    }

    /**
     * Convert entity to DTO
     */
    private SavedCardDTO toDTO(SavedCard card) {
        SavedCardDTO dto = new SavedCardDTO();
        dto.setId(card.getId());
        dto.setCardNumber(card.getMaskedNumber());
        dto.setCardHolder(card.getCardHolder());
        dto.setExpiryMonth(card.getExpiryMonth());
        dto.setExpiryYear(card.getExpiryYear());
        dto.setIsDefault(card.getIsDefault());
        return dto;
    }

    // ========== Validation Methods ==========

    private void validateCardRequest(CardRequestDTO request) {
        if (request.getCardNumber() == null || request.getCardNumber().trim().isEmpty()) {
            throw new RuntimeException("Card number is required");
        }

        String cardNumber = request.getCardNumber().replaceAll("\\s+", "");
        if (!CARD_NUMBER_PATTERN.matcher(cardNumber).matches()) {
            throw new RuntimeException("Card number must be 16 digits");
        }

        if (request.getCardHolder() == null || request.getCardHolder().trim().isEmpty()) {
            throw new RuntimeException("Card holder name is required");
        }

        if (request.getExpiryMonth() == null || request.getExpiryMonth().trim().isEmpty()) {
            throw new RuntimeException("Expiry month is required");
        }

        try {
            int month = Integer.parseInt(request.getExpiryMonth());
            if (month < 1 || month > 12) {
                throw new RuntimeException("Expiry month must be between 01 and 12");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid expiry month format");
        }

        if (request.getExpiryYear() == null || request.getExpiryYear().trim().isEmpty()) {
            throw new RuntimeException("Expiry year is required");
        }

        try {
            int year = Integer.parseInt(request.getExpiryYear());
            int currentYear = Year.now().getValue();
            if (year < currentYear) {
                throw new RuntimeException("Card has expired");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid expiry year format");
        }

        if (request.getCvv() == null || request.getCvv().trim().isEmpty()) {
            throw new RuntimeException("CVV is required");
        }

        if (!CVV_PATTERN.matcher(request.getCvv()).matches()) {
            throw new RuntimeException("CVV must be 3 digits");
        }
    }
}

