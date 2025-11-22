package com.bazunia.vps.service;

import com.bazunia.vps.model.FileRecord;
import com.bazunia.vps.repository.FileRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
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
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            // POPRAWKA: Przypisujemy wynik metody createDirectories do pola klasy.
            // Wcześniej wynik był ignorowany, co powodowało ostrzeżenie "Return value... never used".
            this.fileStorageLocation = Files.createDirectories(path);
        } catch (Exception ex) {
            throw new RuntimeException("Nie można stworzyć folderu do zapisu plików.", ex);
        }
    }

    /**
     * Zapisuje plik na dysku i w bazie danych
     */
    public FileRecord storeFile(MultipartFile file) {
        // POPRAWKA: Obsługa możliwego nulla z getOriginalFilename()
        String rawFileName = file.getOriginalFilename();
        if (rawFileName == null) {
            throw new RuntimeException("Nazwa pliku jest nieprawidłowa (null).");
        }

        // 1. Bezpieczna nazwa pliku
        String originalFileName = StringUtils.cleanPath(rawFileName);

        // 2. Unikalna nazwa, aby uniknąć konfliktów
        // POPRAWKA: Usunięto zbędne .toString() - Java zrobi to automatycznie przy konkatenacji
        String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;

        // 3. Ścieżka zapisu na dysku
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

        try {
            // 4. Zapis pliku na dysk
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 5. Stwórz wpis w bazie danych
            FileRecord fileRecord = new FileRecord(
                    originalFileName,
                    targetLocation.toString(), // Zapisz pełną ścieżkę
                    file.getContentType(),
                    file.getSize()
            );

            // 6. Zapisz metadane w bazie
            return fileRecordRepository.save(fileRecord);

        } catch (IOException ex) {
            throw new RuntimeException("Nie można zapisać pliku " + originalFileName, ex);
        }
    }

    /**
     * Ładuje plik z dysku jako zasób (do pobrania)
     */
    public Resource loadFileAsResource(Long fileId) {
        try {
            FileRecord fileRecord = fileRecordRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("Plik nie znaleziony " + fileId));

            Path filePath = Paths.get(fileRecord.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Plik nie znaleziony " + fileRecord.getOriginalFileName());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Plik nie znaleziony", ex);
        }
    }

    /**
     * Zwraca listę wszystkich plików z bazy
     */
    public List<FileRecord> getAllFiles() {
        return fileRecordRepository.findAll();
    }

    public void deleteFile(Long fileId) {
        FileRecord fileRecord = fileRecordRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Plik nie znaleziony " + fileId));

        try {
            // Usuń fizyczny plik z dysku
            Path filePath = Paths.get(fileRecord.getStoragePath());
            Files.deleteIfExists(filePath);

            // Usuń rekord z bazy
            fileRecordRepository.delete(fileRecord);

        } catch (IOException ex) {
            // Logowanie błędu, ale kontynuujemy usuwanie z bazy, jeśli pliku nie ma
            System.err.println("Błąd podczas próby usunięcia pliku z dysku: " + ex.getMessage());
            fileRecordRepository.delete(fileRecord);

        } catch (Exception ex) {
            throw new RuntimeException("Nie można usunąć pliku i rekordu dla ID: " + fileId, ex);
        }
    }
}