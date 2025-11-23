package com.bazunia.vps.service;

import com.bazunia.vps.model.FileRecord;
import com.bazunia.vps.repository.FileRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final FileRecordRepository fileRecordRepository;

    @Autowired
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir, FileRecordRepository fileRecordRepository) {
        this.fileRecordRepository = fileRecordRepository;
        // Normalizujemy ścieżkę absolutną
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Nie można stworzyć folderu do zapisu plików.", ex);
        }
    }

    /**
     * Zapisuje plik na dysku i w bazie danych
     */
    public FileRecord storeFile(MultipartFile file) {
        String rawFileName = file.getOriginalFilename();
        if (rawFileName == null || rawFileName.contains("..")) {
            throw new SecurityException("Nieprawidłowa nazwa pliku: " + rawFileName);
        }

        // 1. Bezpieczna nazwa pliku
        String originalFileName = StringUtils.cleanPath(rawFileName);
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        try {
            // 2. Ścieżka zapisu na dysku
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName).normalize();

            // 🛡️ BEZPIECZEŃSTWO: Sprawdź, czy ścieżka nie wychodzi poza folder uploads!
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Próba zapisu pliku poza dozwolonym katalogiem!");
            }

            // 3. Zapis pliku na dysk
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 4. Zapisz metadane w bazie
            FileRecord fileRecord = new FileRecord(
                    originalFileName,
                    targetLocation.toString(),
                    file.getContentType(),
                    file.getSize()
            );

            return fileRecordRepository.save(fileRecord);

        } catch (IOException ex) {
            throw new RuntimeException("Błąd zapisu pliku " + originalFileName, ex);
        }
    }

    /**
     * Ładuje plik z dysku jako zasób (do pobrania)
     */
    public Resource loadFileAsResource(Long fileId) {
        try {
            FileRecord fileRecord = fileRecordRepository.findById(fileId)
                    .orElseThrow(() -> new EntityNotFoundException("Plik o ID " + fileId + " nie istnieje w bazie."));

            Path filePath = Paths.get(fileRecord.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new EntityNotFoundException("Plik fizyczny nie został znaleziony: " + fileRecord.getOriginalFileName());
            }
        } catch (Exception ex) {
            throw new EntityNotFoundException("Błąd odczytu pliku: " + ex.getMessage());
        }
    }

    public List<FileRecord> getAllFiles() {
        return fileRecordRepository.findAll();
    }

    public void deleteFile(Long fileId) {
        FileRecord fileRecord = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("Plik o ID " + fileId + " nie istnieje."));

        try {
            Path filePath = Paths.get(fileRecord.getStoragePath());
            Files.deleteIfExists(filePath);
            fileRecordRepository.delete(fileRecord);
        } catch (IOException ex) {
            // Jeśli nie uda się usunąć z dysku, rzucamy błąd, żeby admin wiedział (lub logujemy i usuwamy z bazy)
            throw new RuntimeException("Nie udało się usunąć pliku z dysku.", ex);
        }
    }
}