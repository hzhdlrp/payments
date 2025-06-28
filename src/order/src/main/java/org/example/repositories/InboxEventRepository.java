package org.example.repositories;

import org.example.box.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {
    boolean existsByMessageId(String messageId);
    @Query("SELECT e FROM InboxEvent e WHERE e.processed = false")
    List<InboxEvent> findByProcessedFalse();
}
