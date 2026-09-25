package com.easyprufung.backend.Project.ProjectRepository;

import com.easyprufung.backend.Project.Project;
import com.easyprufung.backend.Project.Upvote;
import com.easyprufung.backend.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpvotesRepository extends JpaRepository<Upvote, Long> {
    boolean existsByUserAndProject(User user, Project project);
    long countByProject(Project project);
}
