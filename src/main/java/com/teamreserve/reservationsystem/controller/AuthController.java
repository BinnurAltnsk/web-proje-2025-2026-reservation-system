package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.AuthRequestDTO;
import com.teamreserve.reservationsystem.dto.AuthResponseDTO;
import com.teamreserve.reservationsystem.dto.RegisterRequestDTO;
import com.teamreserve.reservationsystem.dto.UserResponseDTO;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication Controller",
        description = "Kullanıcı girişi (login) ve kayıt (register) işlemlerini yöneten controller."
)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private UserResponseDTO toUserResponse(ApplicationUser user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getUserRole().name().replace("ROLE_", "").toLowerCase());
        return dto;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Kullanıcı girişi yap",
            description = "Kullanıcı adı ve şifre ile giriş yapar. Başarılı olursa JWT token döner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Giriş başarılı, JWT token döndü",
                    content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Giriş başarısız, kullanıcı adı veya şifre hatalı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    // Dönüş tipini ResponseEntity<?> yaptık ki hem DTO hem de Hata Map'i dönebilelim.
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail().trim().toLowerCase(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            ApplicationUser user = (ApplicationUser) authentication.getPrincipal();

            return ResponseEntity.ok(new AuthResponseDTO(jwt, toUserResponse(user)));

        } catch (BadCredentialsException e) {
            // Şifre veya kullanıcı adı yanlışsa burası çalışır
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Giriş başarısız: E-posta veya şifre hatalı."));
        } catch (Exception e) {
            // Beklenmeyen diğer tüm hatalar (Veritabanı hatası vb.)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Sunucu hatası: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "Yeni kullanıcı kaydı oluştur",
            description = "Yeni bir kullanıcı oluşturur. Kullanıcı adı zaten mevcutsa hata döner."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Kayıt başarılı",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Kayıt başarısız, kullanıcı adı zaten mevcut veya veri hatalı",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            ApplicationUser user = authService.registerUser(registerRequest);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "User registered successfully!",
                            "user", toUserResponse(user)
                    )
            );
        } catch (RuntimeException e) {
            // Service katmanından fırlatılan "Email already exists" gibi hataları yakalar
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Diğer beklenmeyen hatalar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Kayıt sırasında beklenmedik bir hata oluştu."));
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Kullanıcı çıkışı yap",
            description = "İstemciden gelen JWT tokenını mantıksal olarak geçersiz sayar."
    )
    public ResponseEntity<?> logout() {
        try {
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Çıkış işlemi sırasında hata oluştu."));
        }
    }
}