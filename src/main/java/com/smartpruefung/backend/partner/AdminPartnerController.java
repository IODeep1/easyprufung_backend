package com.smartpruefung.backend.partner;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Instant;
@RestController @RequestMapping("/api/admin/v1") @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public class AdminPartnerController {
 private final PartnerService s;private final PartnerStripeService stripe;
 public AdminPartnerController(PartnerService service,PartnerStripeService stripe){s=service;this.stripe=stripe;}
 public static class Change {public String status,reason,promotionCodeId,reference,requestKey;public int commissionBps=2000,discountBps=1000;public Instant paidAt;public boolean confirmed;public long netRefundCents;
  public void setCommissionBps(java.math.BigDecimal value){commissionBps=value.intValueExact();}
  public void setDiscountBps(java.math.BigDecimal value){discountBps=value.intValueExact();}
  public void setNetRefundCents(java.math.BigDecimal value){netRefundCents=value.longValueExact();}
 }
 @GetMapping("/partners") public Object list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.list(q,status,page,size);}
 @GetMapping("/partners/{id}") public Object detail(@PathVariable String id){return s.detail(id);}
 @PutMapping("/partners/{id}/decision") @PreAuthorize("hasRole('ADMIN')") public Object decision(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change b){Object result=s.decision(id,version,b.status,b.reason);if(!"APPROVED".equals(b.status))stripe.suspendCodes(id);return result;}
 @PutMapping("/partners/{id}/commission") @PreAuthorize("hasRole('ADMIN')") public Object commission(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change b){return s.commission(id,version,b.commissionBps,b.discountBps,b.reason);}
 @PostMapping("/partners/{id}/stripe-codes") @PreAuthorize("hasRole('ADMIN')") public Object link(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change b) throws Exception{return stripe.link(id,version,b.promotionCodeId,b.reason);}
 @PutMapping("/partners/{id}/stripe-codes/{codeId}") @PreAuthorize("hasRole('ADMIN')") public Object disable(@PathVariable String id,@PathVariable String codeId,@RequestHeader("If-Match") long version,@RequestBody Change b) throws Exception{return stripe.disable(id,codeId,version,b.reason);}
 @PutMapping("/partners/{id}/payout-verification") @PreAuthorize("hasRole('ADMIN')") public Object verify(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change b){return s.verifyPayout(id,version,b.reason);}
 @PostMapping("/partners/{id}/payout-details") @PreAuthorize("hasRole('ADMIN')") public Object details(@PathVariable String id,@RequestBody Change b){return s.profilePayoutDetails(id,b.reason);}
 @PostMapping("/partners/{id}/payout-requests") @PreAuthorize("hasRole('ADMIN')") public Object request(@PathVariable String id,@RequestBody Change b){return s.adminRequest(id,b.requestKey);}
 @GetMapping("/partner-sales") public Object sales(@RequestParam(defaultValue="") String partnerId,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.adminPage("PartnerSale",partnerId,status,page,size);}
 @PostMapping("/partner-sales/{id}/retry") @PreAuthorize("hasRole('ADMIN')") public Object retrySale(@PathVariable String id,@RequestBody Change b){return stripe.retrySale(id,b.reason);}
 @PostMapping("/partner-sales/{id}/verify-refund") @PreAuthorize("hasRole('ADMIN')") public Object verifyRefund(@PathVariable String id,@RequestHeader("If-Match") long version,@RequestBody Change b) throws Exception{return stripe.verifyRefund(id,version,b.netRefundCents,b.reason);}
 @GetMapping("/partner-payouts") public Object payouts(@RequestParam(defaultValue="") String partnerId,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.adminPage("PartnerPayout",partnerId,status,page,size);}
 @PostMapping("/partner-payouts/{id}/{action:approve|mark-paid|cancel}") @PreAuthorize("hasRole('ADMIN')") public Object payout(@PathVariable String id,@PathVariable String action,@RequestHeader("If-Match") long version,@RequestBody Change b){return s.settle(id,version,action,b.reference,b.paidAt,b.reason,b.confirmed);}
 @PostMapping("/partner-payouts/{id}/details") @PreAuthorize("hasRole('ADMIN')") public Object destination(@PathVariable String id,@RequestBody Change b){return s.payoutDetails(id,b.reason);}
 @GetMapping("/partner-reconciliation") public Object reconciliation(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.page("PartnerJob","status<>'PROCESSED'",java.util.Map.of(),page,size);}
 @PostMapping("/partner-reconciliation/{id}/retry") @PreAuthorize("hasRole('ADMIN')") public Object retry(@PathVariable String id,@RequestBody(required=false) Change b){return stripe.retry(id,b==null?"Manual reconciliation retry":b.reason);}
 @GetMapping("/partners/{id}/audit") public Object audit(@PathVariable String id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.page("PartnerAudit","partnerId=:p",java.util.Map.of("p",id),page,size);}
}
