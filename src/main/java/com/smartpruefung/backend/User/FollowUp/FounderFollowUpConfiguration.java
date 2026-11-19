package com.smartpruefung.backend.User.FollowUp;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class FounderFollowUpConfiguration {
 @Bean(name="founderFollowUpClock")
 public Clock clock(){return Clock.systemUTC();}
}
