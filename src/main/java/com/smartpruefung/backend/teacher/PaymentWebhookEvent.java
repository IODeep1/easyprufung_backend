package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="payment_webhook_events", uniqueConstraints=@UniqueConstraint(columnNames={"event_id"}), indexes={@Index(name="idx_payment_webhook_event_0", columnList="object_id,event_type")})
public class PaymentWebhookEvent extends TeacherRecord {
 @Column(name="event_id", length=255, nullable=false) public String eventId;
 @Column(name="object_id", length=255, nullable=false) public String objectId;
 @Column(name="event_type", length=255, nullable=false) public String eventType;
}
