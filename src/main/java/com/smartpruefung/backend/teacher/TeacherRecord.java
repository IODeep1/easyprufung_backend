package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;
@MappedSuperclass
public abstract class TeacherRecord {
 @Id @Column(length=36) public String id = UUID.randomUUID().toString();
 @Column(name="created_at", nullable=false) public Instant createdAt = Instant.now();
 @Version public long version;
}
