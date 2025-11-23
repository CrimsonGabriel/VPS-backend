package com.bazunia.vps.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StatusService {

    // Tutaj NIE dajemy @Getter, bo musimy ręcznie wywołać .get()
    private final AtomicReference<String> registeredRPiIp = new AtomicReference<>("Brak IP RPi");
    private final AtomicReference<String> lastReportText = new AtomicReference<>("Brak ostatniego meldunku czasu");

    // Tutaj DAJEMY @Getter - Lombok sam zrobi publiczne metody getUniqueAndroidIPs() itp.
    @Getter
    private final Set<String> uniqueAndroidIPs = ConcurrentHashMap.newKeySet();

    @Getter
    private final Set<String> uniqueRpiIPs = ConcurrentHashMap.newKeySet();

    // --- Metody do aktualizacji stanu ---

    public void updateRpiIp(String ip) {
        this.registeredRPiIp.set(ip);
    }

    public void updateLastReport(String report) {
        this.lastReportText.set(report);
    }

    public void addAndroidIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            uniqueAndroidIPs.add(ip);
        }
    }

    public void addRpiIp(String ip) {
        if (ip != null && !ip.isEmpty()) {
            uniqueRpiIPs.add(ip);
        }
    }


    public String getRegisteredRPiIp() {
        return registeredRPiIp.get();
    }

    public String getLastReportText() {
        return lastReportText.get();
    }
}