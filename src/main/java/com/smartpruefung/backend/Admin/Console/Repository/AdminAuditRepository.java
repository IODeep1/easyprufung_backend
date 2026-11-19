package com.smartpruefung.backend.Admin.Console.Repository;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;

public interface AdminAuditRepository
    extends org.springframework.data.jpa.repository.JpaRepository<AdminAudit, Long> {
  org.springframework.data.domain.Page<AdminAudit> findByEntityTypeAndEntityIdOrderByIdDesc(
      String type, String id, org.springframework.data.domain.Pageable page);
}
