package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.MeetingRoomDTO;
import com.teamreserve.reservationsystem.dto.ReservationResponseDTO;
import com.teamreserve.reservationsystem.dto.UserResponseDTO;
import com.teamreserve.reservationsystem.model.MeetingRoom;
import com.teamreserve.reservationsystem.model.Reservation;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import org.springframework.stereotype.Service;

@Service
public class ReservationMapper {

    public UserResponseDTO toUserResponseDTO(ApplicationUser user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getUserRole().name().replace("ROLE_", "").toLowerCase());
        return dto;
    }

    public MeetingRoomDTO toMeetingRoomDTO(MeetingRoom room) {
        MeetingRoomDTO dto = new MeetingRoomDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setLocation(room.getLocation());
        dto.setCapacity(room.getCapacity());
        dto.setDescription(room.getDescription());
        dto.setPrice(room.getPricePerHour());
        dto.setImageUrl(room.getImageUrl());
        dto.setFeatures(room.getFeatures());
        return dto;
    }

    public ReservationResponseDTO toReservationResponseDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setStartTime(reservation.getStartTime());
        dto.setEndTime(reservation.getEndTime());
        dto.setStatus(reservation.getStatus().name().toLowerCase());
        dto.setDate(reservation.getReservationDate());
        dto.setStartTime(reservation.getStartTime());
        dto.setEndTime(reservation.getEndTime());
        dto.setParticipants(reservation.getParticipants());
        dto.setPurpose(reservation.getPurpose());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setDurationHours(reservation.getDurationHours());
        // İç içe olan varlıkları da güvenli DTO'lara çevir
        dto.setUser(toUserResponseDTO(reservation.getUser()));
        dto.setSalon(toMeetingRoomDTO(reservation.getMeetingRoom()));

        return dto;
    }
}