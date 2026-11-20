package com.smartpruefung.backend.User.FollowUp;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import javax.persistence.LockModeType;

public interface FounderFollowUpRepository extends JpaRepository<FounderFollowUp,String> {
 boolean existsByUserUuid(String userUuid);
 Optional<FounderFollowUp> findByUserUuid(String userUuid);
 @Query("select j.id from FounderFollowUp j where j.status='PENDING' and j.nextAttemptAt<=:now order by j.nextAttemptAt,j.id")
 List<String> due(@Param("now") Instant now, Pageable page);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select j from FounderFollowUp j where j.id=:id")
 Optional<FounderFollowUp> lock(@Param("id") String id);
}
