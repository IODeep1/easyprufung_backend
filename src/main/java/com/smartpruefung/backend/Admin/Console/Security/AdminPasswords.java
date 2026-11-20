package com.smartpruefung.backend.Admin.Console.Security;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class AdminPasswords {
  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

  private AdminPasswords() {}

  public static String encode(String raw) {
    if (raw == null || raw.length() < 8 || raw.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("Password must contain 8–72 UTF-8 bytes");
    return ENCODER.encode(raw);
  }

  public static boolean hashed(String value) {
    return value != null && value.startsWith("$2");
  }

  public static boolean matches(String raw, String stored) {
    if (raw == null || stored == null) return false;
    if (hashed(stored)) return ENCODER.matches(raw, stored);
    return false;
  }
}
