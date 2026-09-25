package com.easyprufung.backend.User.Repository;
import com.easyprufung.backend.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User>, QuerydslPredicateExecutor<User> {

    @Query("SELECT a from User a WHERE a.email = ?1")
    User findByEmail(@Param("email") String email);

    @Query("SELECT a from User a WHERE a.uuid = ?1")
    User findByUUID(@Param("uuid") String uuid);

    @Query("SELECT a from User a WHERE a.id = ?1")
    User findById(@Param("id") long id);

    @Query("SELECT a from User a WHERE a.resetPasswordToken = ?1")
    User findByResetPasswordToken(@Param("reset_password_token") String token);
}
