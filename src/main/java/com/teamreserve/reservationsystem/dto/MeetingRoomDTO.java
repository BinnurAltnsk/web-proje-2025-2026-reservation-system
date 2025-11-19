package com.teamreserve.reservationsystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class MeetingRoomDTO {
    private Long id;
    private String name;
    private String location;
    private int capacity;
    private String description;
    private double price;
    private String imageUrl;
    private List<String> features;
}