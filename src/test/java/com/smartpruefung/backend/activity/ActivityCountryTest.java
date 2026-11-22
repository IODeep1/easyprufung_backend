package com.smartpruefung.backend.activity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ActivityCountryTest {
 private ActivityCountry resolver(String header) {return new ActivityCountry(header,new ActivityClientAddress("127.0.0.1/32,::1/128"));}
 @Test void countryUsesOnlyConfiguredHeaderAndNeverLanguage() {
  var request=new MockHttpServletRequest();
  request.addHeader("Accept-Language","de");request.addHeader("cf-ipcountry","FR");
  RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  try {
   assertEquals("FR",resolver("cf-ipcountry").current());
   assertNull(resolver("x-geo-country").current());
   assertNull(resolver("none").current());
   request.addHeader("x-geo-country"," de ");
   assertEquals("DE",resolver("x-geo-country").current());
  } finally {RequestContextHolder.resetRequestAttributes();}
  assertNull(resolver("cf-ipcountry").current());
 }
 @Test void visitorIpNeverProducesACountryAndUntrustedHeadersAreIgnored() {
  var request=new MockHttpServletRequest();
  request.addHeader("X-Real-IP","81.2.69.160");request.addHeader("X-Forwarded-For","2001:218::");
  RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  try {
   assertNull(resolver("cf-ipcountry").current());
   request.addHeader("cf-ipcountry","DE");request.setRemoteAddr("81.2.69.160");
   assertNull(resolver("cf-ipcountry").current());
   request.setRemoteAddr("127.0.0.1");assertEquals("DE",resolver("cf-ipcountry").current());
   assertNull(resolver("none").current());
  } finally {RequestContextHolder.resetRequestAttributes();}
 }
 @Test void unknownAndInvalidCountriesRemainUnknown() {
  for(String value:new String[]{"XX","T1","ZZ","Germany","de,fr","","USA"}) assertNull(ActivityCountry.normalize(value));
  assertNull(ActivityCountry.normalize(null));assertEquals("TN",ActivityCountry.normalize("tn"));
 }
}
