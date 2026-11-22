package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="teacher_audit_events", indexes={@Index(name="idx_teacher_audit_event_0", columnList="actor_id,created_at")})
public class TeacherAuditEvent extends TeacherRecord {
 @Column(name="actor_id", length=255, nullable=false) public String actorId;
 @Column(name="action", length=255, nullable=false) public String action;
 @Column(name="resource_id", length=255, nullable=false) public String resourceId;
}
