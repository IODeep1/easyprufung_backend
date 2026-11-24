package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_webhook_events",indexes={@Index(name="ix_partner_webhook_events_status",columnList="status")})
public class PartnerJob extends PartnerRecord {
@Column(nullable=false,unique=true) public String eventId;
 public String eventType,objectId,status="QUEUED",errorCode;
 public Instant eventAt,nextAttemptAt=Instant.now();
 public int attempts;
 public boolean liveMode;
}
