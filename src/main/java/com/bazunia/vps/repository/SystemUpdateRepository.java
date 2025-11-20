package com.bazunia.vps.repository;

import com.bazunia.vps.model.SystemUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface SystemUpdateRepository extends JpaRepository<SystemUpdate, Long> {
}

