package com.bazunia.vps.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class StatusService {


    private String registeredRPiIp = "Brak IP RPi";
    private String registeredAndroidIp = "Brak IP Androida";
    private String lastReportText = "Brak ostatniego meldunku czasu";


    public synchronized void updateRpiIp(String ip) {
        this.registeredRPiIp = ip;
    }

    public synchronized void updateAndroidIp(String ip) {
        this.registeredAndroidIp = ip;
    }

    public synchronized void updateLastReport(String report) {
        this.lastReportText = report;
    }
}