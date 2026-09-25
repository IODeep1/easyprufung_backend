package com.easyprufung.backend.Project.ProjectRepository;

import com.easyprufung.backend.Project.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistsRepository extends JpaRepository<Waitlist, Integer>, JpaSpecificationExecutor<Waitlist>, QuerydslPredicateExecutor<Waitlist> {

    @Query("SELECT o from Waitlist o WHERE o.id = ?1")
    Waitlist findById(@Param("id") long id);
}