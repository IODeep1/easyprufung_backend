package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="assignment_students", uniqueConstraints=@UniqueConstraint(columnNames={"assignment_id","student_id"}), indexes={@Index(name="idx_assignment_student_0", columnList="student_id")})
public class AssignmentStudent extends TeacherRecord {
 @Column(name="assignment_id", length=255, nullable=false) public String assignmentId;
 @Column(name="student_id", length=255, nullable=false) public String studentId;
 @Column(name="attempt_id", length=255) public String attemptId;
}
