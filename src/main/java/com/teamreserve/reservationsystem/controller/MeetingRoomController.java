package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.AvailabilityResponseDTO;
import com.teamreserve.reservationsystem.dto.MeetingRoomDTO;
import com.teamreserve.reservationsystem.dto.SalonRequestDTO;
import com.teamreserve.reservationsystem.service.SalonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salons")
@Tag(
        name = "Salon Controller",
        description = "Konferans salonları için CRUD ve müsaitlik işlemlerini yöneten controller. " +
                "Kullanıcılar salonları görüntüleyebilir, adminler ise oluşturma, güncelleme ve silme işlemlerini yapabilir."
)
public class MeetingRoomController {

    @Autowired
    private SalonService salonService;

    @GetMapping
    @Operation(
            summary = "Tüm salonları listele",
            description = "Sistemde kayıtlı tüm salonları döner. Bu endpoint herkese açıktır."
    )
    public List<MeetingRoomDTO> getAllRooms() {
        return salonService.getAllSalons();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Belirli bir salonu getir",
            description = "Verilen ID’ye göre salon bilgilerini döner. Salon bulunamazsa 404 Not Found döner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Salon bulundu",
                    content = @Content(schema = @Schema(implementation = MeetingRoomDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Belirtilen ID ile salon bulunamadı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<MeetingRoomDTO> getRoomById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(salonService.getSalonById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/availability")
    @Operation(
            summary = "Salon müsaitliğini kontrol et",
            description = "Belirtilen tarih ve saat aralığı için salon müsaitlik durumunu ve çakışan rezervasyonları döner."
    )
    public ResponseEntity<AvailabilityResponseDTO> checkAvailability(
            @PathVariable Long id,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime
    ) {
        try {
            return ResponseEntity.ok(salonService.checkAvailability(id, date, startTime, endTime));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AvailabilityResponseDTO(false, List.of()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Yeni salon oluştur",
            description = "Sadece admin kullanıcılar tarafından kullanılabilir. Yeni bir salon kaydı oluşturur ve veritabanına ekler."
    )
    public MeetingRoomDTO createRoom(@RequestBody SalonRequestDTO request) {
        return salonService.createSalon(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Salon bilgilerini güncelle",
            description = "Belirli bir ID’ye sahip salonun bilgilerini günceller. Sadece admin kullanıcılar tarafından kullanılabilir."
    )
    public ResponseEntity<MeetingRoomDTO> updateRoom(@PathVariable Long id, @RequestBody SalonRequestDTO request) {
        try {
            return ResponseEntity.ok(salonService.updateSalon(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Salonu sil",
            description = "Belirli bir salonu siler. Sadece admin kullanıcılar bu işlemi yapabilir."
    )
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        try {
            salonService.deleteSalon(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
