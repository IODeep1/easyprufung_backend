package com.smartpruefung.backend.partner;
import java.math.*;
public final class PartnerMath {
 private PartnerMath(){}
 public static long commission(long net,int bps){if(net<0||bps<0||bps>10000)throw new IllegalArgumentException("Invalid commission input.");return BigDecimal.valueOf(net).multiply(BigDecimal.valueOf(bps)).divide(BigDecimal.valueOf(10000),0,RoundingMode.HALF_UP).longValueExact();}
 public static long refundTarget(long commission,long refunded,long gross){if(gross<=0||refunded<0)throw new IllegalArgumentException("Invalid refund input.");return BigDecimal.valueOf(commission).multiply(BigDecimal.valueOf(Math.min(refunded,gross))).divide(BigDecimal.valueOf(gross),0,RoundingMode.HALF_UP).longValueExact();}
}
