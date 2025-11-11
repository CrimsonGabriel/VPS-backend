package com.bazunia.vps.repository;

import com.bazunia.vps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByEmail(String email);

    Optional<User> findByActivationToken(String token);

    Optional<User> findByPasswordResetToken(String token);
}