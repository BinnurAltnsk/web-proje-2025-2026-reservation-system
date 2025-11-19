package com.teamreserve.reservationsystem.service;

import com.teamreserve.reservationsystem.dto.RegisterRequestDTO;
import com.teamreserve.reservationsystem.model.ApplicationUser;
import com.teamreserve.reservationsystem.model.UserRole;
import com.teamreserve.reservationsystem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ApplicationUser registerUser(RegisterRequestDTO registerRequest) {
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new RuntimeException("Error: Email is already registered!");
        }

        ApplicationUser user = new ApplicationUser();
        user.setUsername(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setFullName(registerRequest.getName());
        user.setPhone(registerRequest.getPhone());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setUserRole(UserRole.ROLE_USER);

        return userRepository.save(user);
    }

    // İsteğe bağlı: Admin kullanıcısını ilk çalıştırmada eklemek için
     @PostConstruct
     public void initAdminUser() {
         if (userRepository.findByEmail("admin@system.local").isEmpty()) {
             ApplicationUser admin = new ApplicationUser();
             admin.setUsername("admin@system.local");
             admin.setEmail("admin@system.local");
             admin.setFullName("System Admin");
             admin.setPhone("0000000000");
             admin.setPassword(passwordEncoder.encode("admin123"));
             admin.setUserRole(UserRole.ROLE_ADMIN);
             userRepository.save(admin);
         }
     }
}