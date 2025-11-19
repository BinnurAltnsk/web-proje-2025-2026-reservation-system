package com.teamreserve.reservationsystem.repository;

import com.teamreserve.reservationsystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservationId(Long reservationId);
    List<Payment> findByReservation_User_EmailOrderByCreatedAtDesc(String email);
}

