package com.smartpruefung.backend.partner;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequestMapping("/api/partners") @PreAuthorize("isAuthenticated()")
public class PartnerController {
 private final PartnerService s;
 public PartnerController(PartnerService service){s=service;}
 @PostMapping("/apply") public Object apply(@RequestHeader("Authorization") String a,@RequestBody PartnerService.Input b){return s.apply(a,b);}
 @GetMapping("/me") public Object me(@RequestHeader("Authorization") String a){return s.me(a);}
 @PutMapping("/me") public Object update(@RequestHeader("Authorization") String a,@RequestBody PartnerService.Input b){return s.update(a,b);}
 @PutMapping("/me/payout-method") public Object method(@RequestHeader("Authorization") String a,@RequestBody PartnerService.PayoutInput b){return s.payoutMethod(a,b);}
 @GetMapping("/me/codes") public Object codes(@RequestHeader("Authorization") String a){return s.codes(a);}
 @GetMapping("/me/analytics") public Object analytics(@RequestHeader("Authorization") String a){return s.analytics(a);}
 @GetMapping("/me/overview") public Object overview(@RequestHeader("Authorization") String a){return s.overview(a);}
 @GetMapping("/me/commissions") public Object commissions(@RequestHeader("Authorization") String a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.commissions(a,page,size);}
 @GetMapping("/me/payouts") public Object payouts(@RequestHeader("Authorization") String a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return s.payouts(a,page,size);}
 @PostMapping("/me/payout-requests") public Object request(@RequestHeader("Authorization") String a,@RequestHeader("Idempotency-Key") String key){return s.requestPayout(a,key);}
}
