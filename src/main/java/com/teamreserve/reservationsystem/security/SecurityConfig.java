package com.teamreserve.reservationsystem.security;

import com.teamreserve.reservationsystem.model.UserRole;
import com.teamreserve.reservationsystem.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowCredentials(true);

                    config.setAllowedOrigins(List.of(
                            "http://localhost:5173",
                            "http://localhost:3000"
                    ));

                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // ✅ Swagger UI ve OpenAPI dokümanları için izin
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salons/**").permitAll()

                        // User endpoints
                        .requestMatchers("/api/payment/simulate").hasAuthority(UserRole.ROLE_USER.name())
                        .requestMatchers("/api/reservations/calendar").authenticated()

                        // Admin endpoints
                        .requestMatchers(HttpMethod.POST, "/api/salons").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/salons/**").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/salons/**").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers("/api/reservations/pending").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers("/api/reservations/all").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers("/api/reservations/{id}/approve").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers("/api/reservations/{id}/reject").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers("/api/reservations/upcoming").hasAuthority(UserRole.ROLE_ADMIN.name())

                        // Everything else
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
