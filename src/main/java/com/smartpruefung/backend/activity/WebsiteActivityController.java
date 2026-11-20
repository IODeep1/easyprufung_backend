package com.smartpruefung.backend.activity;

import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Public anonymous telemetry. Identity is a browser identifier, never an account claim. */
@RestController
public class WebsiteActivityController {
 private final ActivityService activity;
 private final ActivityCountry countries;
 public WebsiteActivityController(ActivityService activity,ActivityCountry countries) { this.activity=activity;this.countries=countries; }
 public static class Input {
  @Pattern(regexp="[A-Z]{2}") public String country;
  @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") public String eventId;
  @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") public String visitorId;
  @NotBlank @Pattern(regexp="WEBSITE_PAGE_VIEWED|WEBSITE_CLICKED") public String type;
  @NotBlank @Size(max=300) @Pattern(regexp="/[^?#\\r\\n]*") public String path;
  @Size(max=200) @Pattern(regexp="[^\\p{Cntrl}]*") public String target;
  @Size(max=300) @Pattern(regexp="[^?#\\r\\n]*") public String destination;
  @Size(max=10) @Pattern(regexp="[a-zA-Z-]*") public String language;
 }
 @PostMapping("/public/activity/website") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void record(@Valid @RequestBody Input input) {
  activity.website(input.eventId,input.visitorId,input.type,input.path,input.target,input.destination,input.language,input.country);
 }
 // Production browser traffic goes directly to the app origin. Country is derived
 // at that origin; browser-supplied country metadata is never authoritative here.
 @CrossOrigin(origins={"https://smartpruefung.com","https://www.smartpruefung.com"},allowedHeaders="Content-Type",methods=RequestMethod.POST,allowCredentials="false",maxAge=600)
 @PostMapping("/api/website-activity") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void direct(@Valid @RequestBody Input input) {
  activity.website(input.eventId,input.visitorId,input.type,input.path,input.target,input.destination,input.language,countries.current());
 }
}
