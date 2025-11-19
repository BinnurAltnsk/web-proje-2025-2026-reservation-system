package com.teamreserve.reservationsystem.repository;

import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(ApplicationUser user);
    Optional<UserProfile> findByUserId(Long userId);
}

