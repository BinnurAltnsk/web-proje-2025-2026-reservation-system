package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.ReservationRequestDTO;
import com.teamreserve.reservationsystem.dto.ReservationResponseDTO;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.model.MeetingRoom;
import com.teamreserve.reservationsystem.model.Reservation;
import com.teamreserve.reservationsystem.model.ReservationStatus;
import com.teamreserve.reservationsystem.repository.MeetingRoomRepository;
import com.teamreserve.reservationsystem.repository.ReservationRepository;
import com.teamreserve.reservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationMapper reservationMapper;

    private ApplicationUser loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Date is required");
        }
        return LocalDate.parse(value);
    }

    private LocalTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Time is required");
        }
        return LocalTime.parse(value);
    }

    private boolean hasConflict(Long roomId, LocalDate date, LocalTime start, LocalTime end) {
        return !reservationRepository
                .findConflictingReservations(roomId, date, start, end)
                .isEmpty();
    }

    private double calculateDuration(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            throw new RuntimeException("End time must be after start time");
        }
        return minutes / 60.0;
    }

    private double calculateAmount(double durationHours, double pricePerHour) {
        double amount = durationHours * pricePerHour;
        return Math.round(amount * 100.0) / 100.0;
    }

    private ReservationStatus parseStatus(String status) {
        try {
            return ReservationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid reservation status: " + status);
        }
    }

    public ReservationResponseDTO createReservation(ReservationRequestDTO request, String requesterEmail) {
        if (request.getSalonId() == null) {
            throw new RuntimeException("Salon id is required");
        }
        ApplicationUser user = loadUserByEmail(requesterEmail);
        MeetingRoom room = meetingRoomRepository.findById(request.getSalonId())
                .orElseThrow(() -> new RuntimeException("Salon not found"));

        LocalDate date = parseDate(request.getDate());
        LocalTime start = parseTime(request.getStartTime());
        LocalTime end = parseTime(request.getEndTime());

        if (request.getParticipants() != null && request.getParticipants() > room.getCapacity()) {
            throw new RuntimeException("Requested participant count exceeds salon capacity");
        }

        if (hasConflict(room.getId(), date, start, end)) {
            throw new RuntimeException("Selected time slot is not available");
        }

        double durationHours = calculateDuration(start, end);
        double totalAmount = calculateAmount(durationHours, room.getPricePerHour());

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setMeetingRoom(room);
        reservation.setReservationDate(date);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setParticipants(request.getParticipants());
        reservation.setPurpose(request.getPurpose());
        reservation.setDurationHours(durationHours);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(ReservationStatus.PENDING);

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toReservationResponseDTO(saved);
    }

    public List<ReservationResponseDTO> getMyReservations(String requesterEmail) {
        return reservationRepository.findByUser_EmailOrderByReservationDateDescStartTimeAsc(requesterEmail).stream()
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());
    }

    public ReservationResponseDTO getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        return reservationMapper.toReservationResponseDTO(reservation);
    }

    public ReservationResponseDTO getReservationForUser(Long reservationId, String requesterEmail, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!isAdmin && !reservation.getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new RuntimeException("You are not allowed to view this reservation");
        }

        return reservationMapper.toReservationResponseDTO(reservation);
    }

    public void cancelReservation(Long reservationId, String requesterEmail, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!isAdmin && !reservation.getUser().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new RuntimeException("You can only cancel your own reservations");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    public List<ReservationResponseDTO> getPendingReservations() {
        return reservationRepository.findByStatus(ReservationStatus.PENDING).stream()
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());
    }

    private ReservationResponseDTO updateReservationStatus(Long reservationId, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        reservation.setStatus(status);
        return reservationMapper.toReservationResponseDTO(reservationRepository.save(reservation));
    }

    public ReservationResponseDTO approveReservation(Long reservationId) {
        return updateReservationStatus(reservationId, ReservationStatus.APPROVED);
    }

    public ReservationResponseDTO rejectReservation(Long reservationId) {
        return updateReservationStatus(reservationId, ReservationStatus.REJECTED);
    }

    public List<ReservationResponseDTO> getCalendarReservations() {
        return reservationRepository.findAllByStatus(ReservationStatus.APPROVED).stream()
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ReservationResponseDTO> getUpcomingReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        return reservationRepository.findAllByStatusAndReservationDateBetween(ReservationStatus.APPROVED, today, tomorrow).stream()
                .filter(reservation -> {
                    LocalDateTime startDateTime = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
                    return startDateTime.isAfter(now) && startDateTime.isBefore(now.plusHours(24));
                })
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ReservationResponseDTO> getAllReservations(String status, String date, Long salonId) {
        ReservationStatus desiredStatus = null;
        if (StringUtils.hasText(status)) {
            desiredStatus = parseStatus(status);
        }

        LocalDate desiredDate = StringUtils.hasText(date) ? LocalDate.parse(date) : null;

        return reservationRepository.findAllWithFilters(desiredStatus, salonId, desiredDate).stream()
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());
    }

    public ReservationResponseDTO updateReservationStatus(Long reservationId, String status) {
        ReservationStatus targetStatus = parseStatus(status);
        return updateReservationStatus(reservationId, targetStatus);
    }
}