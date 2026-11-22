package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="teacher_classes", indexes={@Index(name="idx_teacher_class_0", columnList="teacher_id"),@Index(name="idx_teacher_class_1", columnList="invite_hash")})
public class TeacherClass extends TeacherRecord {
 @Column(name="teacher_id", length=255, nullable=false) public String teacherId;
 @Column(name="name", length=255, nullable=false) public String name;
 @Column(name="archived", nullable=false) public boolean archived;
 @Column(name="invite_hash", length=255) public String inviteHash;
 @com.fasterxml.jackson.annotation.JsonIgnore
 @Column(name="invite_token", length=43) public String inviteToken;
 @Column(name="invite_expires_at") public Instant inviteExpiresAt;
}
