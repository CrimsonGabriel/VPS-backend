package com.bazunia.vps.dto;

import com.bazunia.vps.model.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ShareDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareGatewayRequest {
        private Long gatewayId;
        private Long targetUserId;
        private PermissionLevel permissionLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedUserDto {
        private Long userId;
        private String email;
        private PermissionLevel permissionLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPickDto {
        private Long id;
        private String email;
    }
}