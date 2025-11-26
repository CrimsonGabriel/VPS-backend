// 💾 src/main/java/com/bazunia/vps/dto/UserDetailsResponse.java

package com.bazunia.vps.dto;

// Używamy rekordu (record) dla czystości kodu (Java 14+)
public record UserDetailsResponse(
        Long id,
        String email,
        String name,
        String role,
        boolean is2FAEnabled, // frontend potrzebuje tego do statusu
        String avatarUrl      // ⭐️ NOWE POLE
) {}