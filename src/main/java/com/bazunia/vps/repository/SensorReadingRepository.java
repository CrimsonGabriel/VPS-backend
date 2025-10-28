package com.bazunia.vps.repository;

import com.bazunia.vps.model.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    // Magia Spring Data JPA:
    // Na razie zostaw to puste! Sam fakt rozszerzenia JpaRepository
    // automatycznie daje ci gotowe metody:
    // -> .save(SensorReading)    (Create)
    // -> .findById(Long)        (Read)
    // -> .findAll()             (Read)
    // -> .deleteById(Long)      (Delete)
    // To jest wasze pełne CRUD, bez pisania linijki kodu SQL.
}