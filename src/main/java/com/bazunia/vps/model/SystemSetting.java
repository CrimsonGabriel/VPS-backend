package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_settings") // Bezpieczna nazwa tabeli
public class SystemSetting {

    @Id
    @Column(name = "setting_key") // Zmiana nazwy kolumny (key to też keyword)
    private String keyName;

    @Column(name = "setting_value") // Zmiana nazwy kolumny (value to keyword)
    private String value;
}