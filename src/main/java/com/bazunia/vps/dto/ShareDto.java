package com.bazunia.vps.dto;

import com.bazunia.vps.model.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ShareDto {

    // 1. Prośba o udostępnienie bramki
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareGatewayRequest {
        private Long gatewayId;
        private Long targetUserId; // ID użytkownika, któremu udostępniamy
        private PermissionLevel permissionLevel; // VIEW lub FULL_ACCESS
    }

    // 2. Obiekt do wyświetlania listy osób, którym udostępniono bramkę
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedUserDto {
        private Long userId;
        private String email;
        private PermissionLevel permissionLevel;
    }

    // 3. Obiekt do picklisty (wybór użytkownika do udostępnienia)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPickDto {
        private Long id;
        private String email;
        // Możesz dodać np. imię, jeśli masz w bazie
    }
}