package com.smartpruefung.backend.exam.guest;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
@Entity
@Table(name="guest_exams", indexes={@Index(name="idx_guest_client_time",columnList="client_hash,created_at")})
@Data
public class GuestExam {
 @Id @Column(length=36) private String id;
 @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
 @Column(name="client_hash",nullable=false,length=64) private String clientHash;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @org.hibernate.annotations.Type(type="uuid-char")
 @Column(name="session_id",unique=true,length=36) private UUID sessionId;
 @Column(name="finished_at") private Instant finishedAt;
 @Column(name="claimed_user_uuid",length=64) private String claimedUserUuid;
}
