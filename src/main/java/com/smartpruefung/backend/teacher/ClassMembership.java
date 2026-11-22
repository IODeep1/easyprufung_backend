package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="class_memberships", uniqueConstraints=@UniqueConstraint(columnNames={"class_id","student_id"}), indexes={@Index(name="idx_class_membership_0", columnList="student_id,active")})
public class ClassMembership extends TeacherRecord {
 @Column(name="class_id", length=255, nullable=false) public String classId;
 @Column(name="student_id", length=255, nullable=false) public String studentId;
 @Column(name="active", nullable=false) public boolean active;
 @Column(name="consent_version", length=255, nullable=false) public String consentVersion;
 @Column(name="consented_at", nullable=false) public Instant consentedAt;
 @Column(name="left_at") public Instant leftAt;
}
