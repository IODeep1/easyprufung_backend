package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="teacher_seats", uniqueConstraints=@UniqueConstraint(columnNames={"order_id","seat_number"}), indexes={@Index(name="idx_teacher_seat_0", columnList="teacher_id,state")})
public class TeacherSeat extends TeacherRecord {
 @Column(name="order_id", length=255, nullable=false) public String orderId;
 @Column(name="seat_number", nullable=false) public int seatNumber;
 @Column(name="teacher_id", length=255, nullable=false) public String teacherId;
 @Column(name="state", length=255, nullable=false) public String state;
 @Column(name="student_id", length=255) public String studentId;
 @Column(name="activated_at") public Instant activatedAt;
 @Column(name="expires_at") public Instant expiresAt;
}
