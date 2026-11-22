package com.smartpruefung.backend.activity;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ActivityCountry {
 private static final Set<String> CODES=new HashSet<>(Arrays.asList(Locale.getISOCountries()));
 private final String header;
 private final ActivityClientAddress clients;
 public ActivityCountry(@Value("${ACTIVITY_COUNTRY_HEADER:cf-ipcountry}") String header,ActivityClientAddress clients) {
  this.header=header;this.clients=clients;
 }
 public static String normalize(String value) {
  if(value==null) return null;
  String code=value.trim().toUpperCase(Locale.ROOT);
  return CODES.contains(code)?code:null;
 }
 public String current() {
  var context=RequestContextHolder.getRequestAttributes();
  if(!(context instanceof ServletRequestAttributes)) return null;
  var request=((ServletRequestAttributes)context).getRequest();
  if(!"none".equalsIgnoreCase(header) && clients.trusted(request)) {
   String country=normalize(request.getHeader(header));
   if(country!=null) return country;
  }
  return null;
 }
}
