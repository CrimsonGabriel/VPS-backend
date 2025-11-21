package com.bazunia.vps.repository;

import com.bazunia.vps.model.UserGatewayPermission;
import com.bazunia.vps.model.UserGatewayPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserGatewayPermissionRepository extends JpaRepository<UserGatewayPermission, UserGatewayPermissionId> {

    // Znajdź wszystkie uprawnienia dla danej bramki (żeby wyświetlić listę współużytkowników)
    List<UserGatewayPermission> findByGatewayId(Long gatewayId);

    // Znajdź konkretne uprawnienie (np. do usunięcia lub edycji)
    Optional<UserGatewayPermission> findByGatewayIdAndUserId(Long gatewayId, Long userId);

    // Usuń wszystkie uprawnienia dla danej bramki (przydatne przy usuwaniu bramki)
    void deleteAllByGatewayId(Long gatewayId);
}