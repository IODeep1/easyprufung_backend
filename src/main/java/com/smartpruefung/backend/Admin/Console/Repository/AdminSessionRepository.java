package com.smartpruefung.backend.Admin.Console.Repository;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;

public interface AdminSessionRepository
    extends org.springframework.data.jpa.repository.JpaRepository<AdminSession, String> {
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      "delete from AdminSession s where s.expiresAt<:now")
  void deleteExpired(@org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
