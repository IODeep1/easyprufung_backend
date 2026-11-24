package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;
@MappedSuperclass
public abstract class PartnerRecord {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @Column(nullable=false) public Instant createdAt=Instant.now();
 @Version public long version;
}
