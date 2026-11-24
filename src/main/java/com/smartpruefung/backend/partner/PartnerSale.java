package com.smartpruefung.backend.partner;
import javax.persistence.*;
import java.time.Instant;
@Entity @Table(name="partner_attributed_sales",indexes={@Index(name="ix_partner_attributed_sales_partnerId",columnList="partnerId"),@Index(name="ix_partner_attributed_sales_paymentIntentId",columnList="paymentIntentId")})
public class PartnerSale extends PartnerRecord {
public String partnerId;
 @Column(nullable=false,unique=true) public String checkoutSessionId;
 public String paymentIntentId,chargeId,promotionCodeId,priceId,plan,currency,purchaserId;
 public long grossCents,taxCents,netCents,commissionCents,reversedCents,refundedCents;
 public int commissionBps;
 public String state="REQUIRES_REVIEW";
 public String reviewReason="";
 public Instant paidAt,eligibleAt;
 public long verifiedRefundGrossCents=-1,verifiedRefundNetCents;
 public boolean qualified,disputed;
}
