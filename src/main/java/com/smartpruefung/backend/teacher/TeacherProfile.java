package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="teacher_profiles", uniqueConstraints=@UniqueConstraint(columnNames={"user_id"}))
public class TeacherProfile extends TeacherRecord {
 @Column(name="user_id", length=255, nullable=false) public String userId;
 @Column(name="status", length=255, nullable=false) public String status;
}
