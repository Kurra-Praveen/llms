package com.loanplatform.loan.mapper;

import com.loanplatform.loan.dto.CreateLoanRequest;
import com.loanplatform.loan.dto.LoanResponse;
import com.loanplatform.loan.dto.ScheduleResponse;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.RepaymentSchedule;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "loanNumber", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    Loan toEntity(CreateLoanRequest request);

    @Mapping(target = "status", expression = "java(loan.getStatus().name())")
    @Mapping(target = "interestType", expression = "java(loan.getInterestType().name())")
    @Mapping(target = "repaymentFrequency", expression = "java(loan.getRepaymentFrequency().name())")
    @Mapping(target = "totalOutstanding", expression = "java(loan.getTotalOutstanding())")
    @Mapping(target = "borrowerName", source = "borrower.fullName")
    @Mapping(target = "borrowerCode", source = "borrower.borrowerCode")
    @Mapping(target = "schedules", ignore = true)
    LoanResponse toResponse(Loan loan);

    @Mapping(target = "status", expression = "java(schedule.getStatus().name())")
    @Mapping(target = "outstandingAmount", expression = "java(schedule.getOutstandingAmount())")
    ScheduleResponse toScheduleResponse(RepaymentSchedule schedule);

    List<ScheduleResponse> toScheduleResponses(List<RepaymentSchedule> schedules);
}
