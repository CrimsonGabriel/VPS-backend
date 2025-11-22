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
    public void run(String... args) {
        Optional<User> adminOptional = userRepository.findByEmail("admin");

        if (adminOptional.isEmpty()) {
            logger.info(">>> Tworzenie domyślnego użytkownika admin...");

            User adminUser = new User();
            adminUser.setEmail("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setName("Domyślny Administrator");
            adminUser.setRole(Role.ADMIN);

            adminUser.setEnabled(true);

            userRepository.save(adminUser);
            logger.info(">>> Użytkownik admin został stworzony pomyślnie!");
        } else {
            User existingAdmin = adminOptional.get();
            boolean needsUpdate = false;

            if (existingAdmin.getRole() == null || existingAdmin.getRole() != Role.ADMIN) {
                existingAdmin.setRole(Role.ADMIN);
                needsUpdate = true;
                logger.info(">>> Użytkownik admin istniał, Rola została naprawiona na ADMIN.");
            }

            if (!existingAdmin.isEnabled()) {
                existingAdmin.setEnabled(true);
                needsUpdate = true;
                logger.info(">>> Użytkownik admin istniał, ale nie był aktywny. Ustawiono 'enabled = true'.");
            }

            if (needsUpdate) {
                userRepository.save(existingAdmin);
            } else {
                logger.info(">>> Użytkownik admin już istnieje i ma poprawną Rolę/Status. Pomijam zmiany.");
            }
        }
    }
}