package com.smartpruefung.backend.activity;

import javax.validation.Valid;
import javax.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Public anonymous telemetry. Identity is a browser identifier, never an account claim. */
@RestController
@RequestMapping("/public/activity/website")
public class WebsiteActivityController {
 private final ActivityService activity;
 public WebsiteActivityController(ActivityService activity) { this.activity=activity; }
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
 @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT)
 public void record(@Valid @RequestBody Input input) {
  activity.website(input.eventId,input.visitorId,input.type,input.path,input.target,input.destination,input.language,input.country);
 }
}
