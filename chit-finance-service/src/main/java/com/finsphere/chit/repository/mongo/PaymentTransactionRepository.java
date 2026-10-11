package com.finsphere.chit.repository.mongo;

import com.finsphere.chit.domain.PaymentTransaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {

    Page<PaymentTransaction> findByUserId(Long userId, Pageable pageable);
}
