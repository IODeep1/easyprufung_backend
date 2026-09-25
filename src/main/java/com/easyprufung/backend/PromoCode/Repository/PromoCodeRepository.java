package com.easyprufung.backend.PromoCode.Repository;

import com.easyprufung.backend.PromoCode.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Integer>, JpaSpecificationExecutor<PromoCode>, QuerydslPredicateExecutor<PromoCode> {

    @Query("SELECT o from PromoCode o WHERE o.id = ?1")
    PromoCode findById(@Param("id") long id);

    @Query("SELECT a from PromoCode a WHERE a.code = ?1")
    PromoCode findByCode(@Param("code") String code);

}