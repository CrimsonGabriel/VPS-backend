package com.bazunia.vps.controller;

import com.bazunia.vps.model.FileRecord;
import com.bazunia.vps.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController

@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Endpoint do wysyłania pliku: teraz jest pod ścieżką /api/files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        // 1. Przypisujemy wynik do zmiennej (Warning znika!)
        FileRecord savedFile = fileStorageService.storeFile(file);

        // 2. Zwracamy ten obiekt do klienta
        return ResponseEntity.ok(savedFile);
    }

    /**
     * Endpoint do pobierania listy plików: teraz jest pod ścieżką /api/files/
     */
    @GetMapping // Zmieniono z /api/files na / (bo /api/files jest w RequestMapping)
    public ResponseEntity<List<FileRecord>> listAllFiles() {
        List<FileRecord> files = fileStorageService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    /**
     * Endpoint do pobierania konkretnego pliku: teraz jest pod ścieżką /api/files/download/{id}
     */
    @GetMapping("/download/{id}") // Zmieniono z /api/files/download/{id} na /download/{id}
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = fileStorageService.loadFileAsResource(id);

        // Znajdź oryginalną nazwę pliku z bazy (aby przeglądarka wiedziała jak go zapisać)
        String originalFileName = fileStorageService.getAllFiles().stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .map(FileRecord::getOriginalFileName)
                .orElse("downloaded_file");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                .body(resource);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        try {
            fileStorageService.deleteFile(id);
            return ResponseEntity.noContent().build(); // 204 No Content - Sukces
        } catch (RuntimeException e) {
            // Można rozszerzyć o lepszą obsługę błędów (np. 404)
            return ResponseEntity.status(404).build();
        }
    }
}
