package com.bazunia.vps.repository;

import com.bazunia.vps.model.UpdateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UpdateAssignmentRepository extends JpaRepository<UpdateAssignment, Long> {
    List<UpdateAssignment> findByRecipientUserId(Long userId);
    List<UpdateAssignment> findBySystemUpdateId(Long updateId);
}