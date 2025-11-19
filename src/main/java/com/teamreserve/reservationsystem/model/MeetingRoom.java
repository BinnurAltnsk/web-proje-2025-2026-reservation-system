package com.teamreserve.reservationsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting_rooms")
@Data
@NoArgsConstructor
public class MeetingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    private int capacity;

    private String description; // Donanım bilgileri vb.

    private double pricePerHour;

    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "meeting_room_features", joinColumns = @JoinColumn(name = "meeting_room_id"))
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();
}