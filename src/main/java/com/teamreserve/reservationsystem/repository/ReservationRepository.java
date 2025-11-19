package com.teamreserve.reservationsystem.repository;

import com.teamreserve.reservationsystem.model.Reservation;
import com.teamreserve.reservationsystem.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Kullanıcının kendi rezervasyonlarını bulmak için
    List<Reservation> findByUserUsernameOrderByReservationDateDesc(String username);

    // Admin'in bekleyen rezervasyonları görmesi için
    List<Reservation> findByStatus(ReservationStatus status);

    // İş mantığı: Bir odanın belirli bir zaman aralığında çakışan
    @Query("SELECT r FROM Reservation r WHERE r.meetingRoom.id = :roomId " +
            "AND r.reservationDate = :date " +
            "AND r.status IN (com.teamreserve.reservationsystem.model.ReservationStatus.PENDING, " +
            "com.teamreserve.reservationsystem.model.ReservationStatus.APPROVED) " +
            "AND (r.startTime < :endTime AND r.endTime > :startTime)")
    List<Reservation> findConflictingReservations(@Param("roomId") Long roomId,
                                                  @Param("date") LocalDate date,
                                                  @Param("startTime") LocalTime startTime,
                                                  @Param("endTime") LocalTime endTime);

    // YENİ: Takvim görünümü için ONAYLANMIŞ tüm rezervasyonlar
    List<Reservation> findAllByStatus(ReservationStatus status);

    // YENİ: Bildirimler için (Admin Dashboard)
    // Belirli bir durumdaki ve belirli bir zaman aralığında başlayan rezervasyonlar
    List<Reservation> findAllByStatusAndReservationDateBetween(ReservationStatus status, LocalDate start, LocalDate end);

    @Query("""
            SELECT r FROM Reservation r
            WHERE (:status IS NULL OR r.status = :status)
            AND (:salonId IS NULL OR r.meetingRoom.id = :salonId)
            AND (:date IS NULL OR r.reservationDate = :date)
            ORDER BY r.reservationDate DESC, r.startTime ASC
            """)
    List<Reservation> findAllWithFilters(@Param("status") ReservationStatus status,
                                         @Param("salonId") Long salonId,
                                         @Param("date") LocalDate date);

    List<Reservation> findByUser_EmailOrderByReservationDateDescStartTimeAsc(String email);

    List<Reservation> findByMeetingRoomId(Long meetingRoomId);
}