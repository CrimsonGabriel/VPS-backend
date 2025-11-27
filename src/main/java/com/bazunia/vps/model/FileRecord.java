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

    private String originalFileName;
    private String storagePath;
    private String contentType;
    private long size;

    public FileRecord(String originalFileName, String storagePath, String contentType, long size) {
        this.originalFileName = originalFileName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.size = size;
    }
}