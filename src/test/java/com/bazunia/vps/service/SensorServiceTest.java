package com.bazunia.vps.service;

import com.bazunia.vps.dto.SensorCreateRequest;
import com.bazunia.vps.model.Gateway;
import com.bazunia.vps.model.Sensor;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.SensorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private GatewayRepository gatewayRepository;

    @InjectMocks
    private SensorService sensorService;

    @Test
    void createSensor_ShouldSuccess_WhenDataIsValid() {
        // ARRANGE
        Long gatewayId = 10L;
        Long sensorId = 100L;

        SensorCreateRequest request = new SensorCreateRequest();
        request.setId(sensorId);
        request.setGatewayId(gatewayId);
        request.setName("Temp Salon");
        request.setType("DHT11");
        request.setCreatedAt(LocalDateTime.now());
        request.setReportingEnabled(true);

        Gateway mockGateway = new Gateway();
        mockGateway.setId(gatewayId);

        // Mockujemy: Sensor o tym ID nie istnieje
        when(sensorRepository.existsById(sensorId)).thenReturn(false);
        // Mockujemy: Bramka istnieje
        when(gatewayRepository.findById(gatewayId)).thenReturn(Optional.of(mockGateway));
        // Mockujemy: Nazwa w tej bramce jest wolna
        when(sensorRepository.findByNameAndGatewayId(request.getName(), gatewayId)).thenReturn(Optional.empty());
        // Mockujemy: Zapis zwraca obiekt (symulacja zapisu w bazie)
        when(sensorRepository.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        Sensor createdSensor = sensorService.createSensor(request);

        // ASSERT
        assertNotNull(createdSensor);
        assertEquals("Temp Salon", createdSensor.getName());
        assertEquals(mockGateway, createdSensor.getGateway());
        assertTrue(createdSensor.isReportingEnabled());

        verify(sensorRepository).save(any(Sensor.class));
    }

    @Test
    void createSensor_ShouldThrowException_WhenIdAlreadyExists() {
        // ARRANGE
        SensorCreateRequest request = new SensorCreateRequest();
        request.setId(100L);

        when(sensorRepository.existsById(100L)).thenReturn(true);

        // ACT & ASSERT
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> sensorService.createSensor(request));

        assertTrue(exception.getMessage().contains("już istnieje"));
        verify(sensorRepository, never()).save(any());
    }

    @Test
    void createSensor_ShouldThrowException_WhenGatewayNotFound() {
        // ARRANGE
        SensorCreateRequest request = new SensorCreateRequest();
        request.setId(100L);
        request.setGatewayId(99L); // Nieistniejąca bramka

        when(sensorRepository.existsById(100L)).thenReturn(false);
        when(gatewayRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> sensorService.createSensor(request));
    }

    @Test
    void createSensor_ShouldThrowException_WhenNameDuplicatedInGateway() {
        // ARRANGE
        Long gatewayId = 10L;
        String sensorName = "Temp Salon";

        SensorCreateRequest request = new SensorCreateRequest();
        request.setId(100L);
        request.setGatewayId(gatewayId);
        request.setName(sensorName);

        when(sensorRepository.existsById(100L)).thenReturn(false);
        when(gatewayRepository.findById(gatewayId)).thenReturn(Optional.of(new Gateway()));

        // Symulujemy, że sensor o tej nazwie JUŻ jest w bazie
        when(sensorRepository.findByNameAndGatewayId(sensorName, gatewayId))
                .thenReturn(Optional.of(new Sensor()));

        // ACT & ASSERT
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> sensorService.createSensor(request));

        assertTrue(ex.getMessage().contains("już istnieje w tej bramce"));
    }
}