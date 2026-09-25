package com.easyprufung.backend.Project.ProjectRepository;

import com.easyprufung.backend.Project.ContactForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactFormsRepository extends JpaRepository<ContactForm, Integer>, JpaSpecificationExecutor<ContactForm>, QuerydslPredicateExecutor<ContactForm> {

    @Query("SELECT o from ContactForm o WHERE o.id = ?1")
    ContactForm findById(@Param("id") long id);
}