package com.teamreserve.reservationsystem.controller;

import com.teamreserve.reservationsystem.dto.AuthRequestDTO;
import com.teamreserve.reservationsystem.dto.AuthResponseDTO;
import com.teamreserve.reservationsystem.dto.RegisterRequestDTO;
import com.teamreserve.reservationsystem.dto.UserResponseDTO;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.security.JwtTokenProvider;
import com.teamreserve.reservationsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponseDTO> authenticateUser(@RequestBody AuthRequestDTO loginRequest) {
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
                    java.util.Map.of(
                            "message", "User registered successfully!",
                            "user", toUserResponse(user)
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Kullanıcı çıkışı yap",
            description = "İstemciden gelen JWT tokenını mantıksal olarak geçersiz sayar (sunucu tarafında durum tutulmaz)."
    )
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(java.util.Map.of("message", "Logged out successfully"));
    }
}
