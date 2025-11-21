package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.CardRequestDTO;
import com.teamreserve.reservationsystem.dto.SavedCardDTO;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile/cards")
@Tag(
        name = "Card Controller",
        description = "Yönetim için kaydedilmiş kart işlemleri - birden fazla kart desteği"
)
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * Extract user email from JWT token in Authorization header
     */
    private String getUserEmail(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return tokenProvider.getUsernameFromJWT(token);
        }
        throw new RuntimeException("User not authenticated");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Kaydedilmiş kartları listele",
            description = "Kullanıcının tüm kaydedilmiş kartlarını döner. Kart numaraları maskeli olarak gösterilir (**** **** **** 1234)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kartlar başarıyla listelendi",
                    content = @Content(schema = @Schema(implementation = SavedCardDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim - JWT token geçersiz veya eksik",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Kullanıcı bulunamadı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> getCards(HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            List<SavedCardDTO> cards = cardService.getCards(email);
            return ResponseEntity.ok(cards);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Yeni kart ekle",
            description = "Kullanıcı için yeni bir kart kaydeder. CVV doğrulanır ancak saklanmaz. İlk kart otomatik olarak varsayılan kart olur."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kart başarıyla eklendi",
                    content = @Content(schema = @Schema(implementation = SavedCardDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz kart bilgileri",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> addCard(
            @RequestBody CardRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            String email = getUserEmail(httpRequest);
            SavedCardDTO card = cardService.addCard(email, request);
            return ResponseEntity.ok(card);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Kartı sil",
            description = "Kaydedilmiş bir kartı siler. Kart kullanıcıya ait olmalıdır. Varsayılan kart silinirse, bir sonraki kart varsayılan olur."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kart başarıyla silindi",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Bu kart size ait değil",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Kart bulunamadı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> deleteCard(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            cardService.deleteCard(email, id);
            return ResponseEntity.ok(Map.of("message", "Card deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "Varsayılan kartı ayarla",
            description = "Belirtilen kartı varsayılan kart olarak işaretler. Diğer tüm kartlar varsayılan olmaktan çıkar."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Varsayılan kart başarıyla ayarlandı",
                    content = @Content(schema = @Schema(implementation = SavedCardDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Yetkisiz erişim",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Bu kart size ait değil",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Kart bulunamadı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> setDefaultCard(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String email = getUserEmail(request);
            SavedCardDTO card = cardService.setDefaultCard(email, id);
            return ResponseEntity.ok(card);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

