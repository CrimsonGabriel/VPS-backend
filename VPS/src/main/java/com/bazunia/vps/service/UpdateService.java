package com.bazunia.vps.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UpdateService {

    // Map: Key = GatewayID lub "App", Value = "REQUIRED", "OPTIONAL", "NONE"
    private final Map<String, String> updateStatuses = new ConcurrentHashMap<>();

    // Lista bramek, które wymagają powiadomienia (Admin sam to ustawia)
    private final Map<String, Boolean> isNotificationRequired = new ConcurrentHashMap<>();

    // Domyślny status (dla testów)
    public UpdateService() {
        updateStatuses.put("App", "REQUIRED");
        updateStatuses.put("GW-01", "OPTIONAL");
        updateStatuses.put("GW-02", "NONE");

        isNotificationRequired.put("App", true);
        isNotificationRequired.put("GW-01", true);
    }

    public Map<String, String> getUpdateStatuses() {
        return updateStatuses;
    }

    public void setUpdateStatus(String key, String status, boolean ShouldNotify) {
        updateStatuses.put(key, status);
        isNotificationRequired.put(key, ShouldNotify);
    }

    // Metoda dla Androida: Użytkownik zdecydował (na razie to jest tylko logowanie)
    public void recordDecision(String userEmail, String key, String decision) {
        System.out.println(String.format("[UpdateService] Użytkownik %s podjął decyzję dla %s: %s",
                userEmail, key, decision));

        // Jeśli aktualizacja została zaakceptowana, usuń status powiadomienia
        if ("ACCEPTED".equals(decision)) {
            isNotificationRequired.put(key, false);
            // W realnym świecie: wyślij polecenie do bramki
        }
    }

    public boolean isNotificationRequired(String key) {
        return isNotificationRequired.getOrDefault(key, false);
    }
}