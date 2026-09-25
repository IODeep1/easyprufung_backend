package com.easyprufung.backend.Project.ProjectRepository;

import com.easyprufung.backend.Project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectsRepository extends JpaRepository<Project, Integer>, JpaSpecificationExecutor<Project>, QuerydslPredicateExecutor<Project> {

    @Query("SELECT o from Project o WHERE o.id = ?1")
    Project findById(@Param("id") long id);

    @Query("SELECT o from Project o WHERE o.uuid = ?1")
    Project findByUUID(@Param("uuid") String uuid);

    @Query("SELECT p FROM Project p WHERE p.IsPublic = true AND (p.source IS NULL OR p.source != 'external' OR (p.source = 'external' AND p.IsApproved = true))")
    List<Project> findAllPublicProjects();
}