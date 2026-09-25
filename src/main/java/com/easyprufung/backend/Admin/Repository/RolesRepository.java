package com.easyprufung.backend.Admin.Repository;

import com.easyprufung.backend.Admin.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface RolesRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role>, QuerydslPredicateExecutor<Role> {

    @Query("SELECT r from Role r WHERE r.name = ?1")
    public Role findByName(String name);
}
