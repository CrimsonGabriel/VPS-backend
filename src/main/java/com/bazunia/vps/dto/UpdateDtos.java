package com.bazunia.vps.dto;

import com.bazunia.vps.model.SystemUpdate;
import com.bazunia.vps.model.UpdateAssignment;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public class UpdateDtos {
    @Data
    public static class CreateUpdateRequest {
        private String title;
        private String description;
        private String version;
        private SystemUpdate.UpdateUrgency urgency;
        private SystemUpdate.UpdateTargetType targetType;
        private Long targetUserId;
        private Long targetGatewayId;
        private Long targetSensorId;
    }

    @Data
    public static class UpdateSummaryDto {
        private Long id;
        private String title;
        private String version;
        private SystemUpdate.UpdateUrgency urgency;
        private SystemUpdate.UpdateTargetType targetType;
        private LocalDateTime createdAt;
        private List<AssignmentDto> assignments;
    }

    @Data
    public static class AssignmentDto {
        private Long id;
        private Long targetId;
        private Long recipientUserId;
        private UpdateAssignment.AssignmentStatus status;
        private int deferCount;
        private LocalDateTime lastActionAt;
    }

    @Data
    public static class ClientUpdateResponse {
        private Long assignmentId;
        private String title;
        private String description;
        private String version;
        private SystemUpdate.UpdateUrgency urgency;
        private SystemUpdate.UpdateTargetType targetType;
        private Long targetId;
    }

    @Data
    public static class UpdateStatusRequest {
        private UpdateAssignment.AssignmentStatus status;
    }
}