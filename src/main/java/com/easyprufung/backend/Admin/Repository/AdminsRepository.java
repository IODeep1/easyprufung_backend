package com.easyprufung.backend.Admin.Repository;
import com.easyprufung.backend.Admin.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminsRepository extends JpaRepository<Admin, Integer>, JpaSpecificationExecutor<Admin>, QuerydslPredicateExecutor<Admin> {

    @Query("SELECT a from Admin a WHERE a.email = ?1")
    Admin findByEmail(@Param("email") String email);

    @Query("SELECT a from Admin a WHERE a.id = ?1")
    Admin findById(@Param("id") long id);
}
