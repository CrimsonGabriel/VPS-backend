package com.bazunia.vps.service;

import com.bazunia.vps.dto.UpdateDtos;
import com.bazunia.vps.model.*;
import com.bazunia.vps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateService {

    private final SystemUpdateRepository systemUpdateRepository;
    private final UpdateAssignmentRepository assignmentRepository;
    private final GatewayRepository gatewayRepository;
    private final SensorRepository sensorRepository;


    /**
     * Tworzy aktualizację i przypisuje ją do wybranych celów.
     */
    @Transactional
    public SystemUpdate createUpdate(UpdateDtos.CreateUpdateRequest request) {
        // 1. Utwórz definicję aktualizacji
        SystemUpdate update = SystemUpdate.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .version(request.getVersion())
                .urgency(request.getUrgency())
                .targetType(request.getTargetType())
                .build();

        update = systemUpdateRepository.save(update);

        // 2. Rozdziel logikę w zależności od celu
        if (request.getTargetType() == SystemUpdate.UpdateTargetType.APP) {
            // Aktualizacja aplikacji dla konkretnego usera
            createAssignment(update, request.getTargetUserId(), request.getTargetUserId());

        } else if (request.getTargetType() == SystemUpdate.UpdateTargetType.GATEWAY) {
            if (request.getTargetGatewayId() != null) {
                // Pojedyncza bramka
                Gateway g = gatewayRepository.findById(request.getTargetGatewayId())
                        .orElseThrow(() -> new RuntimeException("Gateway not found"));

                // Przyjmuję, że Gateway ma metodę getOwnerUserId() lub User w relacji
                Long ownerId = g.getOwner() != null ? g.getOwner().getId() : null;
                createAssignment(update, g.getId(), ownerId);
            }
            // Usunięto pusty blok else (logika dla WSZYSTKICH bramek nie była zaimplementowana)

        } else if (request.getTargetType() == SystemUpdate.UpdateTargetType.SENSOR) {
            if (request.getTargetSensorId() != null) {
                Sensor s = sensorRepository.findById(request.getTargetSensorId())
                        .orElseThrow(() -> new RuntimeException("Sensor not found"));
                // Znajdź właściciela przez bramkę
                Long ownerId = s.getGateway().getOwner().getId();
                createAssignment(update, s.getId(), ownerId);
            }
        }

        return update;
    }

    private void createAssignment(SystemUpdate update, Long targetId, Long recipientId) {
        UpdateAssignment assignment = UpdateAssignment.builder()
                .systemUpdate(update)
                .targetId(targetId)
                .recipientUserId(recipientId)
                .status(UpdateAssignment.AssignmentStatus.PENDING)
                .deferCount(0)
                .build();
        assignmentRepository.save(assignment);
    }

    /**
     * Pobiera aktualizacje dla panelu admina.
     */
    public List<UpdateDtos.UpdateSummaryDto> getAllUpdatesSummary() {
        List<SystemUpdate> updates = systemUpdateRepository.findAll();
        return updates.stream().map(u -> {
            UpdateDtos.UpdateSummaryDto dto = new UpdateDtos.UpdateSummaryDto();
            dto.setId(u.getId());
            dto.setTitle(u.getTitle());
            dto.setVersion(u.getVersion());
            dto.setUrgency(u.getUrgency());
            dto.setTargetType(u.getTargetType());
            dto.setCreatedAt(u.getCreatedAt());

            List<UpdateAssignment> assignments = assignmentRepository.findBySystemUpdateId(u.getId());
            List<UpdateDtos.AssignmentDto> assignmentDtos = assignments.stream().map(a -> {
                UpdateDtos.AssignmentDto ad = new UpdateDtos.AssignmentDto();
                ad.setId(a.getId());
                ad.setTargetId(a.getTargetId());
                ad.setRecipientUserId(a.getRecipientUserId());
                ad.setStatus(a.getStatus());
                ad.setDeferCount(a.getDeferCount());
                ad.setLastActionAt(a.getLastActionAt());
                return ad;
            }).collect(Collectors.toList());

            dto.setAssignments(assignmentDtos);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Resend / Retry dla Admina.
     * Resetuje status do PENDING dla istniejącego assignmentu.
     */
    @Transactional
    public void resendUpdate(Long assignmentId) {
        UpdateAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        assignment.setStatus(UpdateAssignment.AssignmentStatus.PENDING);
        // Opcjonalnie: wyzeruj licznik odroczeń, jeśli admin wymusza ponowienie
        // assignment.setDeferCount(0);
        assignment.setLastActionAt(java.time.LocalDateTime.now());
        assignmentRepository.save(assignment);
    }

    /**
     * Dla API Klienta (Android): Pobiera oczekujące aktualizacje dla usera.
     */
    public List<UpdateDtos.ClientUpdateResponse> getPendingUpdatesForUser(Long userId) {
        List<UpdateAssignment> assignments = assignmentRepository.findByRecipientUserId(userId);

        return assignments.stream()
                .filter(a -> a.getStatus() != UpdateAssignment.AssignmentStatus.COMPLETED)
                .map(a -> {
                    UpdateDtos.ClientUpdateResponse dto = new UpdateDtos.ClientUpdateResponse();
                    dto.setAssignmentId(a.getId());
                    dto.setTitle(a.getSystemUpdate().getTitle());
                    dto.setDescription(a.getSystemUpdate().getDescription());
                    dto.setVersion(a.getSystemUpdate().getVersion());
                    dto.setUrgency(a.getSystemUpdate().getUrgency());
                    dto.setTargetType(a.getSystemUpdate().getTargetType());
                    dto.setTargetId(a.getTargetId());
                    return dto;
                }).collect(Collectors.toList());
    }

    /**
     * Dla API Klienta (Android): Aktualizacja statusu (Accept / Defer).
     */
    @Transactional
    public void updateAssignmentStatus(Long assignmentId, UpdateAssignment.AssignmentStatus newStatus) {
        UpdateAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (newStatus == UpdateAssignment.AssignmentStatus.DEFERRED) {
            assignment.setDeferCount(assignment.getDeferCount() + 1);
            // Usunięto pusty blok if sprawdzający deferCount > 1 (tylko komentarze, brak logiki)
        }

        assignment.setStatus(newStatus);
        assignment.setLastActionAt(java.time.LocalDateTime.now());
        assignmentRepository.save(assignment);
    }
}