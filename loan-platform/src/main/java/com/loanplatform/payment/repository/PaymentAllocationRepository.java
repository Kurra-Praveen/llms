package com.loanplatform.payment.repository;

import com.loanplatform.payment.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {

    @Query("SELECT pa FROM PaymentAllocation pa WHERE pa.paymentId = :paymentId")
    List<PaymentAllocation> findByPaymentId(@Param("paymentId") UUID paymentId);

    @Query("SELECT pa FROM PaymentAllocation pa WHERE pa.scheduleId = :scheduleId")
    List<PaymentAllocation> findByScheduleId(@Param("scheduleId") UUID scheduleId);
}
