package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.ReservationRequestDTO;
import com.teamreserve.reservationsystem.dto.ReservationResponseDTO;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(
        name = "Reservation Controller",
        description = "Toplantı odası rezervasyon işlemlerini yöneten controller. " +
                "Kullanıcılar rezervasyon oluşturabilir, kendi rezervasyonlarını görüntüleyebilir ve iptal edebilir. " +
                "Admin kullanıcılar onay, reddetme ve bekleyen rezervasyonları yönetebilir."
)
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    // Token'dan kullanıcı adını çıkaran yardımcı metod
    private String getUsernameFromToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUsernameFromJWT(token);
        }
        throw new RuntimeException("User not authenticated");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(
            summary = "Yeni rezervasyon oluştur",
            description = "Giriş yapmış kullanıcı tarafından çağrılır. " +
                    "Belirtilen toplantı odası, başlangıç ve bitiş zamanı bilgilerine göre rezervasyon oluşturur. " +
                    "Rezervasyon durumu varsayılan olarak **PENDING** (beklemede) olur."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rezervasyon başarıyla oluşturuldu",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz istek veya oda bulunamadı",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Kullanıcının yetkisi yok",
                    content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
            )
    })
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequestDTO requestDTO, HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            ReservationResponseDTO reservation = reservationService.createReservation(requestDTO, email);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }


    // CRUD: Read (Kullanıcının kendi rezervasyonları)
    @GetMapping("/my-reservations")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Kullanıcının kendi rezervasyonlarını listele",
            description = "JWT token üzerinden kullanıcı adı alınır ve sadece o kullanıcıya ait rezervasyonlar döner."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Kullanıcının rezervasyonları başarıyla listelendi",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Kullanıcının yetkisi yok",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
                    )
            )
    })
    public List<ReservationResponseDTO> getMyReservations(HttpServletRequest request) {
        String email = getUsernameFromToken(request);
        return reservationService.getMyReservations(email);
    }

    // CRUD: Read - Tek rezervasyon detayı
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getReservationById(@PathVariable Long id, HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            return ResponseEntity.ok(reservationService.getReservationForUser(id, email, isAdmin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // CRUD: Delete (Kullanıcı veya Admin) -> durumunu CANCELLED yap
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Rezervasyonu iptal et",
            description = "Kullanıcı kendi rezervasyonunu, admin ise tüm rezervasyonları iptal edebilir."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rezervasyon başarıyla iptal edildi"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Kullanıcının yetkisi yok",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Rezervasyon bulunamadı",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
                    )
            )
    })
    public ResponseEntity<?> cancelReservation(@PathVariable Long id, HttpServletRequest request) {
        try {
            String email = getUsernameFromToken(request);
            boolean isAdmin = request.isUserInRole("ROLE_ADMIN");
            reservationService.cancelReservation(id, email, isAdmin);
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Reservation cancelled successfully",
                    "refundAmount", 0
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long salonId
    ) {
        try {
            return ResponseEntity.ok(reservationService.getAllReservations(status, date, salonId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // React takvim kütüphanesi için veri kaynağı
    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Takvim görünümü için rezervasyon verilerini getir",
            description = "Tüm onaylanmış rezervasyonları React takvim bileşeninde görüntülemek için döner."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Onaylanmış rezervasyonlar başarıyla listelendi",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Yetkisiz erişim",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")
                    )
            )
    })
    public List<ReservationResponseDTO> getCalendarData() {
        return reservationService.getCalendarReservations();
    }

    // CRUD: Read (Admin - Bekleyen rezervasyonlar)
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Bekleyen rezervasyonları listele (Admin)",
            description = "Onay bekleyen tüm rezervasyonları listeler. Sadece admin kullanıcılar erişebilir."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Bekleyen rezervasyonlar listelendi",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Yetkisiz erişim"
            )
    })
    public List<ReservationResponseDTO> getPendingReservations() {
        return reservationService.getPendingReservations();
    }

    // CRUD: Update (Admin - Onay)
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Rezervasyonu onayla (Admin)",
            description = "Admin tarafından onaylanan rezervasyonun durumunu APPROVED olarak günceller."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rezervasyon başarıyla onaylandı",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Yetkisiz erişim"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Rezervasyon bulunamadı"
            )
    })
    public ResponseEntity<ReservationResponseDTO> approveReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.approveReservation(id));
    }

    // CRUD: Update (Admin - Red)
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Rezervasyonu reddet (Admin)",
            description = "Admin tarafından reddedilen rezervasyonun durumunu REJECTED olarak günceller."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rezervasyon başarıyla reddedildi",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Yetkisiz erişim"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Rezervasyon bulunamadı"
            )
    })
    public ResponseEntity<ReservationResponseDTO> rejectReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.rejectReservation(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Rezervasyon durumunu güncelle",
            description = "Admin onay veya reddetme işlemlerini tek endpoint üzerinden gerçekleştirebilir."
    )
    public ResponseEntity<?> updateReservationStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload
    ) {
        String status = payload.get("status");
        if (!StringUtils.hasText(status)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Status is required"));
        }

        try {
            return ResponseEntity.ok(reservationService.updateReservationStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // Admin Dashboard: Gelecek 24 saat içindeki rezervasyonlar
    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Yaklaşan rezervasyonları listele (Admin)",
            description = "Önümüzdeki 24 saat içinde gerçekleşecek onaylanmış rezervasyonları döner. Admin panelinde bildirim olarak kullanılır."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Yaklaşan rezervasyonlar başarıyla listelendi",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReservationResponseDTO.class)
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Yetkisiz erişim"
            )
    })
    public List<ReservationResponseDTO> getUpcomingReservations() {
        return reservationService.getUpcomingReservations();
    }

}
