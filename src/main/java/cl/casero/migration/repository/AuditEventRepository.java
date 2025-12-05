package cl.casero.migration.repository;

import cl.casero.migration.domain.AuditEvent;
import cl.casero.migration.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    @EntityGraph(attributePaths = "user")
    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<AuditEvent> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("select ae from AuditEvent ae where ae.eventType = :eventType "
        + "and function('jsonb_extract_path_text', ae.payload, 'type') = :payloadType")
    Page<AuditEvent> findByEventTypeAndPayloadType(
        @Param("eventType") AuditEventType eventType,
        @Param("payloadType") String payloadType,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "user")
    @Query("select ae from AuditEvent ae where function('jsonb_extract_path_text', ae.payload, 'type') = :payloadType")
    Page<AuditEvent> findByPayloadType(
        @Param("payloadType") String payloadType,
        Pageable pageable
    );
}
