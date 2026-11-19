package com.smartpruefung.backend.activity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ActivityCountryTest {
 @Test void countryUsesOnlyConfiguredHeaderAndNeverLanguage() {
  var request=new MockHttpServletRequest();
  request.addHeader("Accept-Language","de");request.addHeader("cf-ipcountry","FR");
  RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  try {
   assertEquals("FR",new ActivityCountry("cf-ipcountry").current());
   assertNull(new ActivityCountry("x-geo-country").current());
   assertNull(new ActivityCountry("none").current());
   request.addHeader("x-geo-country"," de ");
   assertEquals("DE",new ActivityCountry("x-geo-country").current());
  } finally {RequestContextHolder.resetRequestAttributes();}
  assertNull(new ActivityCountry("cf-ipcountry").current());
 }
 @Test void unknownAndInvalidCountriesRemainUnknown() {
  for(String value:new String[]{"XX","T1","ZZ","Germany","de,fr","","USA"}) assertNull(ActivityCountry.normalize(value));
  assertNull(ActivityCountry.normalize(null));assertEquals("TN",ActivityCountry.normalize("tn"));
 }
}
