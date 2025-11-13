package com.bazunia.vps.dto;

/**
 * To DTO jest używane jako odpowiedź dla GET /data/android.
 * Łączy dane z SensorReading (wartość, czas) z Sensor (typ, id).
 */
public record SensorReadingResponseDto(
        String gatewayId,
        String sensorId,
        String type, // <<< Pole, którego brakowało
        String value,
        long timestamp,
        Long id // ID samego odczytu
) {
    /**
     * Konstruktor używany przez Spring Data JPA (JPA Projections) do
     * mapowania wyników zapytania SQL bezpośrednio na ten obiekt.
     */
    public SensorReadingResponseDto(Long gatewayId, Long sensorId, String type, String value, long timestamp, Long id) {
        // Konwertuje ID na String, tak jak oczekuje tego VpsClientService
        this(String.valueOf(gatewayId), String.valueOf(sensorId), type, value, timestamp, id);
    }
}