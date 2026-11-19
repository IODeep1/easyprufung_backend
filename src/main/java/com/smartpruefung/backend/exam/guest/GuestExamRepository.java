package com.smartpruefung.backend.exam.guest;

import java.time.Instant;
import java.util.*;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported=false)
public interface GuestExamRepository extends JpaRepository<GuestExam,String> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select g from GuestExam g where g.tokenHash=:hash")
 Optional<GuestExam> lockToken(@Param("hash") String hash);
 long countByClientHashAndCreatedAtAfter(String hash,Instant after);
}
