package com.bazunia.vps.repository;

import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.Sensor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SensorRepositoryTest {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private GatewayRepository gatewayRepository;

    @Test
    void findByNameAndGatewayId_ShouldReturnSensor_WhenExists() {
        // ARRANGE
        Gateway gateway = new Gateway();
        gateway.setName("Test Gateway");

        gateway.setStatus("OFFLINE");
        gateway.setFolder("Test Folder");
        gatewayRepository.save(gateway);

        Sensor sensor = new Sensor();
        sensor.setId(101L);
        sensor.setName("Czujnik Ognia");
        sensor.setGateway(gateway);
        sensor.setReportingEnabled(true);
        sensor.setCreatedAt(LocalDateTime.now());
        sensorRepository.save(sensor);

        // ACT
        Optional<Sensor> found = sensorRepository.findByNameAndGatewayId("Czujnik Ognia", gateway.getId());

        // ASSERT
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Czujnik Ognia");
    }

    @Test
    void findByNameAndGatewayId_ShouldReturnEmpty_WhenNotExists() {
        // ARRANGE
        Gateway gateway = new Gateway();
        gateway.setName("Empty Gateway");
        gateway.setStatus("OFFLINE");
        gateway.setFolder("Test");
        gatewayRepository.save(gateway);

        // ACT
        Optional<Sensor> found = sensorRepository.findByNameAndGatewayId("Nieistniejący", gateway.getId());

        // ASSERT
        assertThat(found).isEmpty();
    }
}