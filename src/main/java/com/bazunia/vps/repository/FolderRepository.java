package com.bazunia.vps.repository;

import com.bazunia.vps.model.Folder;
import com.bazunia.vps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    // Znajduje foldery dla danego użytkownika
    List<Folder> findByOwner(User owner);

    // Znajduje konkretny folder użytkownika (do weryfikacji uprawnień)
    Optional<Folder> findByIdAndOwner(Long id, User owner);

    // ⭐️ ZMIANA ZAPYTANIA I NAZWY METODY (Krok 4) ⭐️
    // Pobiera JEDEN folder od razu z bramkami I CZUJNIKAMI
    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.gateways LEFT JOIN FETCH f.sensors WHERE f.id = :id AND f.owner = :owner")
    Optional<Folder> findByIdAndOwnerWithGatewaysAndSensors(Long id, User owner);

    // ⭐️ ZMIANA ZAPYTANIA I NAZWY METODY (Krok 4) ⭐️
    // Pobiera WSZYSTKIE foldery od razu z bramkami I CZUJNIKAMI
    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.gateways LEFT JOIN FETCH f.sensors WHERE f.owner = :owner")
    List<Folder> findByOwnerWithGatewaysAndSensors(User owner);


}