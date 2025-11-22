package com.bazunia.vps.repository;

import com.bazunia.vps.model.UserGatewayPermission;
import com.bazunia.vps.model.UserGatewayPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserGatewayPermissionRepository extends JpaRepository<UserGatewayPermission, UserGatewayPermissionId> {

    List<UserGatewayPermission> findByGatewayId(Long gatewayId);
    Optional<UserGatewayPermission> findByGatewayIdAndUserId(Long gatewayId, Long userId);
    void deleteAllByGatewayId(Long gatewayId);
}