package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.*;
import com.teamreserve.reservationsystem.service.SalonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/salons")
@Tag(name = "Salon Controller")
public class MeetingRoomController {

    @Autowired private SalonService salonService;

    @GetMapping
    public ResponseEntity<?> getAllRooms() {
        try {
            List<MeetingRoomDTO> rooms = salonService.getAllSalons();
            return ResponseEntity.ok(ApiResponse.success("Salonlar başarıyla listelendi", rooms));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Salonlar getirilemedi", "INTERNAL_ERROR", Collections.singletonList(e.getMessage())));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomById(@PathVariable Long id) {
        try {
            MeetingRoomDTO room = salonService.getSalonById(id);
            return ResponseEntity.ok(ApiResponse.success("Salon bilgileri getirildi", room));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Salon bulunamadı", "NOT_FOUND", Collections.singletonList("ID: " + id + " olan salon mevcut değil")));
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> checkAvailability(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            AvailabilityResponseDTO availability = salonService.checkAvailability(id, date, startTime, endTime);
            return ResponseEntity.ok(ApiResponse.success("Müsaitlik durumu kontrol edildi", availability));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Salon not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiErrorResponse.error("Salon bulunamadı", "NOT_FOUND", null));
            }
            return ResponseEntity.badRequest()
                    .body(ApiErrorResponse.error("Geçersiz istek parametreleri", "BAD_REQUEST", Collections.singletonList(e.getMessage())));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createRoom(@RequestBody SalonRequestDTO request) {
        try {
            MeetingRoomDTO createdRoom = salonService.createSalon(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Salon başarıyla oluşturuldu", createdRoom));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiErrorResponse.error("Salon oluşturulamadı", "INTERNAL_ERROR", Collections.singletonList(e.getMessage())));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody SalonRequestDTO request) {
        try {
            MeetingRoomDTO updatedRoom = salonService.updateSalon(id, request);
            return ResponseEntity.ok(ApiResponse.success("Salon başarıyla güncellendi", updatedRoom));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Salon bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            salonService.deleteSalon(id);
            // 204 No Content (İçerik yok, başarılı silme)
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.error("Salon bulunamadı", "NOT_FOUND", Collections.singletonList(e.getMessage())));
        }
    }
}