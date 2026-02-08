package com.loanplatform.payment.engine;

import com.loanplatform.loan.entity.RepaymentSchedule;
import com.loanplatform.loan.entity.ScheduleStatus;
import com.loanplatform.payment.entity.PaymentAllocation;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class PaymentAllocationEngine {

    @Data
    @Builder
    public static class AllocationResult {
        private BigDecimal totalPrincipalAllocated;
        private BigDecimal totalInterestAllocated;
        private BigDecimal totalPenaltyAllocated;
        private BigDecimal excessAmount;
        private List<PaymentAllocation> allocations;
        private List<RepaymentSchedule> updatedSchedules;
    }

    public AllocationResult allocatePayment(
            UUID paymentId,
            UUID tenantId,
            BigDecimal paymentAmount,
            List<RepaymentSchedule> unpaidSchedules) {

        log.info("Allocating payment of {} across {} schedules", paymentAmount, unpaidSchedules.size());

        unpaidSchedules.sort(Comparator
                .comparing(RepaymentSchedule::getDueDate)
                .thenComparing(RepaymentSchedule::getInstallmentNumber));

        List<PaymentAllocation> allocations = new ArrayList<>();
        List<RepaymentSchedule> updatedSchedules = new ArrayList<>();

        BigDecimal remainingAmount = paymentAmount;
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalPenalty = BigDecimal.ZERO;

        for (RepaymentSchedule schedule : unpaidSchedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            ScheduleAllocation allocation = allocateToSchedule(schedule, remainingAmount);

            if (allocation.totalAllocated.compareTo(BigDecimal.ZERO) > 0) {
                schedule.applyPayment(
                        allocation.principalAllocated,
                        allocation.interestAllocated,
                        allocation.penaltyAllocated
                );

                PaymentAllocation paymentAllocation = PaymentAllocation.builder()
                        .tenantId(tenantId)
                        .paymentId(paymentId)
                        .scheduleId(schedule.getId())
                        .principalAllocated(allocation.principalAllocated)
                        .interestAllocated(allocation.interestAllocated)
                        .penaltyAllocated(allocation.penaltyAllocated)
                        .totalAllocated(allocation.totalAllocated)
                        .build();

                allocations.add(paymentAllocation);
                updatedSchedules.add(schedule);

                totalPrincipal = totalPrincipal.add(allocation.principalAllocated);
                totalInterest = totalInterest.add(allocation.interestAllocated);
                totalPenalty = totalPenalty.add(allocation.penaltyAllocated);
                remainingAmount = remainingAmount.subtract(allocation.totalAllocated);
            }
        }

        log.info("Payment allocation complete: P={}, I={}, Penalty={}, Excess={}",
                totalPrincipal, totalInterest, totalPenalty, remainingAmount);

        return AllocationResult.builder()
                .totalPrincipalAllocated(totalPrincipal)
                .totalInterestAllocated(totalInterest)
                .totalPenaltyAllocated(totalPenalty)
                .excessAmount(remainingAmount.max(BigDecimal.ZERO))
                .allocations(allocations)
                .updatedSchedules(updatedSchedules)
                .build();
    }

    private ScheduleAllocation allocateToSchedule(RepaymentSchedule schedule, BigDecimal availableAmount) {
        BigDecimal penaltyAllocated = BigDecimal.ZERO;
        BigDecimal interestAllocated = BigDecimal.ZERO;
        BigDecimal principalAllocated = BigDecimal.ZERO;
        BigDecimal remaining = availableAmount;

        BigDecimal penaltyOutstanding = schedule.getPenaltyOutstanding();
        if (penaltyOutstanding.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(BigDecimal.ZERO) > 0) {
            penaltyAllocated = penaltyOutstanding.min(remaining);
            remaining = remaining.subtract(penaltyAllocated);
        }

        BigDecimal interestOutstanding = schedule.getInterestOutstanding();
        if (interestOutstanding.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(BigDecimal.ZERO) > 0) {
            interestAllocated = interestOutstanding.min(remaining);
            remaining = remaining.subtract(interestAllocated);
        }

        BigDecimal principalOutstanding = schedule.getPrincipalOutstanding();
        if (principalOutstanding.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(BigDecimal.ZERO) > 0) {
            principalAllocated = principalOutstanding.min(remaining);
            remaining = remaining.subtract(principalAllocated);
        }

        BigDecimal total = penaltyAllocated.add(interestAllocated).add(principalAllocated);

        return new ScheduleAllocation(
                principalAllocated.setScale(2, RoundingMode.HALF_UP),
                interestAllocated.setScale(2, RoundingMode.HALF_UP),
                penaltyAllocated.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private record ScheduleAllocation(
            BigDecimal principalAllocated,
            BigDecimal interestAllocated,
            BigDecimal penaltyAllocated,
            BigDecimal totalAllocated
    ) {}

    public void reverseAllocation(RepaymentSchedule schedule, PaymentAllocation allocation) {
        BigDecimal newPrincipalPaid = schedule.getPrincipalPaid().subtract(allocation.getPrincipalAllocated());
        BigDecimal newInterestPaid = schedule.getInterestPaid().subtract(allocation.getInterestAllocated());
        BigDecimal newPenaltyPaid = schedule.getPenaltyPaid().subtract(allocation.getPenaltyAllocated());

        schedule.setPrincipalPaid(newPrincipalPaid.max(BigDecimal.ZERO));
        schedule.setInterestPaid(newInterestPaid.max(BigDecimal.ZERO));
        schedule.setPenaltyPaid(newPenaltyPaid.max(BigDecimal.ZERO));
        schedule.setTotalPaid(schedule.getPrincipalPaid().add(schedule.getInterestPaid()).add(schedule.getPenaltyPaid()));

        if (schedule.getTotalPaid().compareTo(BigDecimal.ZERO) == 0) {
            schedule.setStatus(ScheduleStatus.PENDING);
            schedule.setPaidDate(null);
        } else {
            schedule.setStatus(ScheduleStatus.PARTIAL);
        }
    }
}
