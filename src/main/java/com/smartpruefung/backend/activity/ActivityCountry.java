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
 public ActivityCountry(@Value("${ACTIVITY_COUNTRY_HEADER:cf-ipcountry}") String header) { this.header=header; }
 public static String normalize(String value) {
  if(value==null) return null;
  String code=value.trim().toUpperCase(Locale.ROOT);
  return CODES.contains(code)?code:null;
 }
 public String current() {
  if("none".equalsIgnoreCase(header)) return null;
  var context=RequestContextHolder.getRequestAttributes();
  return context instanceof ServletRequestAttributes ? normalize(((ServletRequestAttributes)context).getRequest().getHeader(header)) : null;
 }
}
