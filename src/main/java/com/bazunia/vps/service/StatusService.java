package com.bazunia.vps.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@Getter
public class StatusService {

    private String registeredRPiIp = "Brak IP RPi";
    private String lastReportText = "Brak ostatniego meldunku czasu";

    // Zbiór IP KLIENTÓW MOBILNYCH (Android/Przeglądarka)
    private final Set<String> uniqueAndroidIPs = Collections.synchronizedSet(new HashSet<>());

    // ⭐️ NOWE POLE: Zbiór unikalnych IP RPi (bramki) ⭐️
    private final Set<String> uniqueRpiIPs = Collections.synchronizedSet(new HashSet<>());

    // --- Metody do aktualizacji stanu ---

    public synchronized void updateRpiIp(String ip) {
        this.registeredRPiIp = ip;
    }

    public synchronized void updateLastReport(String report) {
        this.lastReportText = report;
    }

    /** Dodaje IP klienta mobilnego (Android). */
    public void addAndroidIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            uniqueAndroidIPs.add(ip);
        }
    }

    /** ⭐️ NOWA METODA: Dodaje IP klienta RPi (Bramki). ⭐️ */
    public void addRpiIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            uniqueRpiIPs.add(ip);
        }
    }

    public Set<String> getUniqueAndroidIPs() {
        return new HashSet<>(uniqueAndroidIPs);
    }

    public Set<String> getUniqueRpiIPs() {
        return new HashSet<>(uniqueRpiIPs); // Zbiór RPi, może być użyty w przyszłości
    }
}