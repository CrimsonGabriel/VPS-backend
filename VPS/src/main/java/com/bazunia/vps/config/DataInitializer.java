package com.bazunia.vps.config;

import com.bazunia.vps.model.User;
import com.bazunia.vps.model.Role;
import com.bazunia.vps.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Logger;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Logger logger = Logger.getLogger(DataInitializer.class.getName());

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Optional<User> adminOptional = userRepository.findByEmail("admin");

        if (adminOptional.isEmpty()) {
            logger.info(">>> Tworzenie domyślnego użytkownika admin...");

            User adminUser = new User();
            adminUser.setEmail("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setName("Domyślny Administrator");
            adminUser.setRole(Role.ADMIN);

            userRepository.save(adminUser);
            logger.info(">>> Użytkownik admin został stworzony pomyślnie!");
        } else {
            // Bezpieczna logika: naprawa roli istniejącego usera
            User existingAdmin = adminOptional.get();
            if (existingAdmin.getRole() == null || existingAdmin.getRole() != Role.ADMIN) {
                existingAdmin.setRole(Role.ADMIN);
                userRepository.save(existingAdmin);
                logger.info(">>> Użytkownik admin istniał, ale Rola została ustawiona/naprawiona na ADMIN.");
            } else {
                logger.info(">>> Użytkownik admin już istnieje i ma poprawną Rolę. Pomijam zmiany.");
            }
        }
    }
}