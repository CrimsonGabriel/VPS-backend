package com.bazunia.vps.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(name = "setting_key")
    private String keyName;

    @Column(name = "setting_value")
    private String value;
}