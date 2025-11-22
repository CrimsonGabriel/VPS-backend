package com.bazunia.vps.repository;

import com.bazunia.vps.model.Folder;
import com.bazunia.vps.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByIdAndOwner(Long id, User owner);
    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.gateways LEFT JOIN FETCH f.sensors WHERE f.id = :id AND f.owner = :owner")
    Optional<Folder> findByIdAndOwnerWithGatewaysAndSensors(Long id, User owner);
    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.gateways LEFT JOIN FETCH f.sensors WHERE f.owner = :owner")
    List<Folder> findByOwnerWithGatewaysAndSensors(User owner);


}