package com.smartpruefung.backend.activity;

import javax.persistence.*;
import java.time.Instant;

/** Append-only event; no foreign key so deleting an account does not break analytics. */
@Entity
@Table(name="user_activity", indexes={
 @Index(name="idx_activity_time",columnList="occurred_at"),
 @Index(name="idx_activity_user_time",columnList="user_uuid,occurred_at"),
 @Index(name="idx_activity_type_time",columnList="event_type,occurred_at")})
public class ActivityEvent {
 @Id @Column(length=150) private String id;
 @Column(name="user_uuid",nullable=false,length=64) private String userUuid;
 @Column(name="event_type",nullable=false,length=32) private String eventType;
 @Column(name="occurred_at",nullable=false) private Instant occurredAt;
 @Column(length=36) private String sessionId;
 @Column(length=32) private String plan;
 @Column(length=100) private String source;
 @Column(name="page_path",length=300) private String pagePath;
 @Column(length=200) private String target;
 @Column(length=300) private String destination;
 @Column(length=10) private String language;
 @Column(length=2) private String country;
}
