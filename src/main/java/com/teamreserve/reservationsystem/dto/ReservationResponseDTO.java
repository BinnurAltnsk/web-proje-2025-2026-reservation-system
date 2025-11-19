package com.teamreserve.reservationsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservationResponseDTO {
    private Long id;
    private UserResponseDTO user;
    private MeetingRoomDTO salon;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Integer participants;
    private String purpose;
    private Double totalAmount;
    private Double durationHours;
    private String status;

    @JsonProperty("reservationId")
    public Long getReservationId() {
        return this.id;
    }
}