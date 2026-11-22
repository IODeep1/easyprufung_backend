package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="seat_offers", indexes={@Index(name="idx_seat_offer_0", columnList="student_id,state"),@Index(name="idx_seat_offer_1", columnList="seat_id"),@Index(name="idx_seat_offer_2", columnList="class_id"),@Index(name="idx_seat_offer_3", columnList="state,expires_at")})
public class SeatOffer extends TeacherRecord {
 @Column(name="seat_id", length=255, nullable=false) public String seatId;
 @Column(name="class_id", length=255, nullable=false) public String classId;
 @Column(name="student_id", length=255, nullable=false) public String studentId;
 @Column(name="state", length=255, nullable=false) public String state;
 @Column(name="expires_at", nullable=false) public Instant expiresAt;
 @Column(name="resolved_at") public Instant resolvedAt;
}
