package com.teamreserve.reservationsystem.repository;

import com.teamreserve.reservationsystem.model.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
    // Gelecekte filtreleme için metodlar eklenebilir
}