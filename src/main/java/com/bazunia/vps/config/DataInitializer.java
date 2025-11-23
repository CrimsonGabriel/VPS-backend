package com.bazunia.vps.config;

import com.bazunia.vps.model.User;
import com.bazunia.vps.model.Role;
import com.bazunia.vps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Optional<User> adminOptional = userRepository.findByEmail("admin");

        if (adminOptional.isEmpty()) {
            log.info(">>> Tworzenie domyślnego użytkownika admin...");

            User adminUser = new User();
            adminUser.setEmail("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setName("Domyślny Administrator");
            adminUser.setRole(Role.ADMIN);
            adminUser.setEnabled(true);

            userRepository.save(adminUser);
            log.info(">>> Użytkownik admin został stworzony pomyślnie!");
        } else {
            User existingAdmin = adminOptional.get();
            boolean needsUpdate = false;

            if (existingAdmin.getRole() == null || existingAdmin.getRole() != Role.ADMIN) {
                existingAdmin.setRole(Role.ADMIN);
                needsUpdate = true;
                log.info(">>> Użytkownik admin istniał. Naprawiono rolę na ADMIN.");
            }

            if (!existingAdmin.isEnabled()) {
                existingAdmin.setEnabled(true);
                needsUpdate = true;
                log.info(">>> Użytkownik admin istniał (nieaktywny). Ustawiono enabled=true.");
            }

            if (needsUpdate) {
                userRepository.save(existingAdmin);
            } else {
                log.info(">>> Użytkownik admin istnieje i jest poprawny. Pomijam.");
            }
        }
    }
}