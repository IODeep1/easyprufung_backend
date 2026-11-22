package com.smartpruefung.backend.teacher;
import java.util.Map;
import java.util.List;
public final class SeatPricing {
 private SeatPricing() {}
 public static int unit(int quantity) {
  if(quantity<1 || quantity>1000) throw new IllegalArgumentException("Choose between 1 and 1000 seats.");
  return quantity>=50?1199:quantity>=25?1399:quantity>=10?1599:1999;
 }
 public static String tier(int quantity) {
  unit(quantity);return quantity>=50?"50plus":quantity>=25?"25plus":quantity>=10?"10plus":"standard";
 }
 public static List<Map<String,Object>> schedule() {
  return List.of(
   Map.of("code","standard","label","1–9","minimum",1,"maximum",9,"unitPriceCents",1999),
   Map.of("code","10plus","label","10–24","minimum",10,"maximum",24,"unitPriceCents",1599),
   Map.of("code","25plus","label","25–49","minimum",25,"maximum",49,"unitPriceCents",1399),
   Map.of("code","50plus","label","50+","minimum",50,"maximum",1000,"unitPriceCents",1199));
 }
 public static Map<String,Object> quote(int quantity) {
  int unit=unit(quantity);
  return Map.of("quantity",quantity,"unitPriceCents",unit,"grossTotalCents",Math.multiplyExact(unit,quantity),"currency","EUR","plan","TEN_EXAMS_60_DAYS","pricingTier",tier(quantity));
 }
}
