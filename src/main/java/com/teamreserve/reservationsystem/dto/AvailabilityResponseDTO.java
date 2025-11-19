package com.teamreserve.reservationsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AvailabilityResponseDTO {
    private boolean available;
    private List<ReservationResponseDTO> conflictingReservations;
}

