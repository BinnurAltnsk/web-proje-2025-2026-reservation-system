package com.teamreserve.reservationsystem.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SalonRequestDTO {
    private String name;
    private String location;
    private int capacity;
    private double price;
    private String description;
    private String imageUrl;
    private List<String> features = new ArrayList<>();
}

