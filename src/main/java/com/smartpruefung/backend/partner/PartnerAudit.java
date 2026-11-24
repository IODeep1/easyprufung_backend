package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_audit_events",indexes={@Index(name="ix_partner_audit_events_partnerId",columnList="partnerId")})
public class PartnerAudit extends PartnerRecord {
public String partnerId,actor,action;
 @Column(length=2000) public String reason;
}
