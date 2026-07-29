package com.example.usermanagement.repository;

import java.util.List;
import java.util.UUID;

import com.example.usermanagement.enums.OutboxStatus;
import com.example.usermanagement.event.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
