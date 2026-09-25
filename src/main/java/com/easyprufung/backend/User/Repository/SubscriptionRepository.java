package com.easyprufung.backend.User.Repository;
import com.easyprufung.backend.User.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer>, JpaSpecificationExecutor<Subscription>, QuerydslPredicateExecutor<Subscription> {

    @Query("SELECT o from Subscription o WHERE o.id = ?1")
    Subscription findById(@Param("id") long id);

    @Query("SELECT a from Subscription a WHERE a.customerId = ?1")
    Subscription findByCustomerId(@Param("customer_id") String customerId);

    @Query("SELECT a from Subscription a WHERE a.customerEmail = ?1")
    Subscription findByCustomerEmail(@Param("customer_email") String customerEmail);
}