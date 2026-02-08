package com.loanplatform.payment.mapper;

import com.loanplatform.payment.dto.PaymentResponse;
import com.loanplatform.payment.entity.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    @Mapping(target = "paymentMethod", expression = "java(payment.getPaymentMethod().name())")
    @Mapping(target = "loanNumber", source = "loan.loanNumber")
    @Mapping(target = "allocations", ignore = true)
    PaymentResponse toResponse(Payment payment);
}
