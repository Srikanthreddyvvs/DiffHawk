package com.DiffHawk.repository;

import com.DiffHawk.domain.OutboxEvent;
import com.DiffHawk.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent,Long> {
    List<OutboxEvent> findByStatus(OutboxStatus status);
}
