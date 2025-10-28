package com.bazunia.vps.controller;

import com.bazunia.vps.dto.DataRequest;
import com.bazunia.vps.dto.RegistrationRequest;
import com.bazunia.vps.dto.StatusResponse;
import com.bazunia.vps.repository.SensorReadingRepository;
import com.bazunia.vps.service.StatusService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {


    private static final String SECRET_PASSWORD = "ZMIEN_TO_HASLO_XD";


    private final StatusService statusService;
    private final SensorReadingRepository sensorRepository;

    @Autowired
    public ApiController(StatusService statusService, SensorReadingRepository sensorRepository) {
        this.statusService = statusService;
        this.sensorRepository = sensorRepository;
    }



    @PostMapping("/register/rasp")
    public ResponseEntity<String> registerRaspberry(@RequestBody RegistrationRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String ip = request.getRemoteAddr(); // Pobranie IP
        statusService.updateRpiIp(ip + ":" + body.port());
        return ResponseEntity.ok("IP RPi zarejestrowane.");
    }

    @PostMapping("/register/android")
    public ResponseEntity<String> registerAndroid(@RequestBody RegistrationRequest body, HttpServletRequest request) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }
        String ip = request.getRemoteAddr();
        statusService.updateAndroidIp(ip + ":" + body.port());
        return ResponseEntity.ok("IP Androida zarejestrowane.");
    }




    @PostMapping("/data")
    public ResponseEntity<String> receiveData(@RequestBody DataRequest body) {
        if (!SECRET_PASSWORD.equals(body.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo.");
        }


        sensorRepository.saveAll(body.sensors());

        return ResponseEntity.ok("Dane czujnikow zapisane w bazie.");
    }


    @GetMapping("/data/android")
    public ResponseEntity<?> getSensorData(@RequestHeader("Password") String password) {
        if (!SECRET_PASSWORD.equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nieprawidlowe haslo w naglowku.");
        }


        return ResponseEntity.ok(sensorRepository.findAll());
    }


    @GetMapping("/")
    public String getRootStatus() {

        // tu zaimplementować pełną stronę HTML,
        // tak jak w Node.js [cite: 8-23], używając pliku index.html w src/main/resources/static
        return "Serwer VPS (Spring Boot) działa!";
    }


    @GetMapping("/status/json")
    public StatusResponse getStatusJson() {

        return new StatusResponse(
                statusService.getLastReportText(),
                statusService.getRegisteredRPiIp(),
                statusService.getRegisteredAndroidIp()
        );
    }
}