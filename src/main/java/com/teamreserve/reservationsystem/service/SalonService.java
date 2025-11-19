package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.AvailabilityResponseDTO;
import com.teamreserve.reservationsystem.dto.MeetingRoomDTO;
import com.teamreserve.reservationsystem.dto.ReservationResponseDTO;
import com.teamreserve.reservationsystem.dto.SalonRequestDTO;
import com.teamreserve.reservationsystem.model.MeetingRoom;
import com.teamreserve.reservationsystem.repository.MeetingRoomRepository;
import com.teamreserve.reservationsystem.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalonService {

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationMapper reservationMapper;

    private MeetingRoomDTO toDto(MeetingRoom room) {
        return reservationMapper.toMeetingRoomDTO(room);
    }

    public List<MeetingRoomDTO> getAllSalons() {
        return meetingRoomRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public MeetingRoomDTO getSalonById(Long id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salon not found"));
        return toDto(room);
    }

    private void applyRequest(MeetingRoom room, SalonRequestDTO request) {
        room.setName(request.getName());
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setPricePerHour(request.getPrice());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());
        List<String> features = request.getFeatures() != null
                ? new ArrayList<>(request.getFeatures())
                : new ArrayList<>();
        room.setFeatures(features);
    }

    public MeetingRoomDTO createSalon(SalonRequestDTO request) {
        MeetingRoom room = new MeetingRoom();
        applyRequest(room, request);
        return toDto(meetingRoomRepository.save(room));
    }

    public MeetingRoomDTO updateSalon(Long id, SalonRequestDTO request) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salon not found"));
        applyRequest(room, request);
        return toDto(meetingRoomRepository.save(room));
    }

    public void deleteSalon(Long id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salon not found"));
        meetingRoomRepository.delete(room);
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

    public AvailabilityResponseDTO checkAvailability(Long salonId, String date, String startTime, String endTime) {
        MeetingRoom room = meetingRoomRepository.findById(salonId)
                .orElseThrow(() -> new RuntimeException("Salon not found"));

        LocalDate localDate = parseDate(date);
        LocalTime start = parseTime(startTime);
        LocalTime end = parseTime(endTime);

        List<ReservationResponseDTO> conflicts = reservationRepository.findConflictingReservations(room.getId(), localDate, start, end)
                .stream()
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());

        return new AvailabilityResponseDTO(conflicts.isEmpty(), conflicts);
    }

    public List<ReservationResponseDTO> getSalonReservations(Long salonId) {
        return reservationRepository.findByMeetingRoomId(salonId).stream()
                .map(reservationMapper::toReservationResponseDTO)
                .collect(Collectors.toList());
    }
}

