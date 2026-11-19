package com.smartpruefung.backend.Admin.Console.Repository;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;

public interface AdminSubscriptionStateRepository
    extends org.springframework.data.jpa.repository.JpaRepository<AdminSubscriptionState, Long> {}
