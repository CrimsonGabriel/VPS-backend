package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName; // np. "mój-raport.pdf"
    private String storagePath;      // np. "/var/uploads/12345-moj-raport.pdf"
    private String contentType;      // np. "application/pdf"
    private long size;               // np. 102400 (w bajtach)

    public FileRecord(String originalFileName, String storagePath, String contentType, long size) {
        this.originalFileName = originalFileName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.size = size;
    }
}