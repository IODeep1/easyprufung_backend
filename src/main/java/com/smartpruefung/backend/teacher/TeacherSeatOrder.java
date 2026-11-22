package com.smartpruefung.backend.teacher;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="teacher_seat_orders", indexes={@Index(name="idx_teacher_seat_order_0", columnList="teacher_id"),@Index(name="idx_teacher_seat_order_1", columnList="payment_intent_id")})
public class TeacherSeatOrder extends TeacherRecord {
 @Column(name="teacher_id", length=255, nullable=false) public String teacherId;
 @Column(name="quantity", nullable=false) public int quantity;
 @Column(name="unit_price_cents", nullable=false) public int unitPriceCents;
 @Column(name="total_cents", nullable=false) public int totalCents;
 @Column(name="currency", length=255, nullable=false) public String currency;
 @Column(name="status", length=255, nullable=false) public String status;
 @Column(name="stripe_session_id", length=255, unique=true) public String stripeSessionId;
 @Column(name="payment_intent_id", length=255) public String paymentIntentId;
 @Column(name="stripe_price_id", length=255) public String stripePriceId;
 @Column(name="pricing_tier", length=32) public String pricingTier;
 @Column(name="stripe_customer_id", length=255) public String stripeCustomerId;
 @Column(name="receipt_url", length=1000) public String receiptUrl;
 @Column(name="support_review", nullable=false) public boolean supportReview;
}
