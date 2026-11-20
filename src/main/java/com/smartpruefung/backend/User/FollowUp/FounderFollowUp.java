package com.smartpruefung.backend.User.FollowUp;

import javax.persistence.*;
import java.time.Instant;
import lombok.Data;

@Entity
@Table(name="founder_follow_up", uniqueConstraints=@UniqueConstraint(name="uk_founder_follow_up_user",columnNames="user_uuid"),
 indexes=@Index(name="idx_founder_follow_up_due",columnList="status,next_attempt_at"))
@Data
public class FounderFollowUp {
 @Id @Column(length=36) private String id;
 @Column(name="user_uuid",nullable=false,length=64) private String userUuid;
 @Column(name="due_at",nullable=false) private Instant dueAt;
 @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
 @Column(nullable=false,length=16) private String status="PENDING";
 @Column(nullable=false) private int attempts;
 @Column(name="sent_at") private Instant sentAt;
 @Column(name="last_error",length=100) private String lastError;
}
