package com.teamreserve.reservationsystem.dto;

import lombok.Data;

@Data
public class ReservationRequestDTO {
    private Long salonId;
    private String date;
    private String startTime;
    private String endTime;
    private Integer participants;
    private String purpose;
    private Double totalAmount;
    private Double duration;
}