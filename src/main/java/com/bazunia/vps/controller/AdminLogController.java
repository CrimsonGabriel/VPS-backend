package com.bazunia.vps.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    @GetMapping
    public ResponseEntity<List<String>> getAppLogs(@RequestParam(defaultValue = "out") String type) {
        String logFilePath = switch (type) {
            case "error" -> "/home/ubuntu/.pm2/logs/bazunia-app-spring-error.log";
            case "server" -> "server.log";
            default -> "/home/ubuntu/.pm2/logs/bazunia-app-spring-out.log";
        };

        int linesToRead = 300;
        try (Stream<String> lines = Files.lines(Paths.get(logFilePath))) {
            List<String> allLines = lines.toList();
            int start = Math.max(0, allLines.size() - linesToRead);
            List<String> lastLines = allLines.subList(start, allLines.size());

            Pattern ansiPattern = Pattern.compile("\\x1B\\[[0-9;]*[a-zA-Z]");
            List<String> cleanLogs = lastLines.stream()
                    .map(line -> ansiPattern.matcher(line).replaceAll(""))
                    .toList();

            return ResponseEntity.ok(cleanLogs);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Błąd odczytu: " + e.getMessage()));
        }
    }
}