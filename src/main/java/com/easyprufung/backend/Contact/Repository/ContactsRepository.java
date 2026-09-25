package com.easyprufung.backend.Contact.Repository;

import com.easyprufung.backend.Contact.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactsRepository extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact>, QuerydslPredicateExecutor<Contact> {

}
