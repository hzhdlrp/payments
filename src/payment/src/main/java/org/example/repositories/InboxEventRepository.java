package org.example.repositories;

import org.example.box.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM InboxEvent e WHERE e.messageId = :messageId")
    boolean existsByMessageId(@Param("messageId") String messageId);
    @Query("SELECT e FROM InboxEvent e WHERE e.processed = false")
    List<InboxEvent> findByProcessedFalse();
}
