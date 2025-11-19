package com.teamreserve.reservationsystem.repository;

import com.teamreserve.reservationsystem.model.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCardRepository extends JpaRepository<SavedCard, Long> {
    
    /**
     * Find all saved cards for a user
     */
    List<SavedCard> findByUserId(Long userId);
    
    /**
     * Find a specific card by ID that belongs to a user (for authorization check)
     */
    Optional<SavedCard> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Find the default card for a user
     */
    Optional<SavedCard> findByUserIdAndIsDefaultTrue(Long userId);
    
    /**
     * Count cards for a user (useful to check if this is the first card)
     */
    long countByUserId(Long userId);
}

