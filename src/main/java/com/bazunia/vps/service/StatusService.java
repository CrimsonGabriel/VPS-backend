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
    private String registeredAndroidIp = "Brak IP Androida"; // Dla starego endpointu RPi
    private String lastReportText = "Brak ostatniego meldunku czasu";

    // NOWY ZBIÓR (SET) - Zamiast uniqueAndroidIPs z server.js
    // Jest "synchronized" aby był bezpieczny przy wielu żądaniach na raz
    private final Set<String> uniqueAndroidIPs = Collections.synchronizedSet(new HashSet<>());

    // --- Metody do aktualizacji stanu (bezpieczne wątkowo) ---

    public synchronized void updateRpiIp(String ip) {
        this.registeredRPiIp = ip;
    }

    public synchronized void updateAndroidIp(String ip) {
        this.registeredAndroidIp = ip;
    }

    public synchronized void updateLastReport(String report) {
        this.lastReportText = report;
    }

    // --- NOWA METODA, KTÓREJ BRAKOWAŁO W AuthService ---

    /**
     * Dodaje IP klienta Android (z logowania Google) do zbioru unikalnych IP.
     * @param ip Adres IP klienta.
     */
    public void addAndroidIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            uniqueAndroidIPs.add(ip);
            System.out.println("[StatusService] Dodano IP Androida: " + ip + ". Łącznie: " + uniqueAndroidIPs.size());
        }
    }

    // Metoda do pobierania listy IP (dla /status/json)
    public Set<String> getUniqueAndroidIPs() {
        // Zwracamy kopię, aby nikt z zewnątrz nie modyfikował naszej listy
        return new HashSet<>(uniqueAndroidIPs);
    }
}