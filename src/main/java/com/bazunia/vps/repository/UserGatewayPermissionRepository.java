package com.bazunia.vps.repository;

import com.bazunia.vps.model.UserGatewayPermission;
import com.bazunia.vps.model.UserGatewayPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGatewayPermissionRepository extends JpaRepository<UserGatewayPermission, UserGatewayPermissionId> {
}