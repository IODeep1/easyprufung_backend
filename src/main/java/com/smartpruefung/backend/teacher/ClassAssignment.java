package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="class_assignments", indexes={@Index(name="idx_class_assignment_0", columnList="class_id")})
public class ClassAssignment extends TeacherRecord {
 @Column(name="class_id", length=255, nullable=false) public String classId;
 @Column(name="exam_code", length=255, nullable=false) public String examCode;
 @Column(name="due_at") public Instant dueAt;
}
