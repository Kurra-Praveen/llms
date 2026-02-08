package com.loanplatform.scheduler;

import com.loanplatform.common.config.TenantContext;
import com.loanplatform.loan.entity.Loan;
import com.loanplatform.loan.entity.RepaymentSchedule;
import com.loanplatform.loan.entity.ScheduleStatus;
import com.loanplatform.loan.repository.LoanRepository;
import com.loanplatform.loan.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyScheduler {

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    @Scheduled(cron = "${app.scheduler.overdue-check:0 0 1 * * ?}")
    @Transactional
    public void processOverdueSchedules() {
        log.info("Starting daily overdue processing job");
        LocalDate today = LocalDate.now();

        List<RepaymentSchedule> overdueSchedules = scheduleRepository.findOverdueSchedules(today);
        log.info("Found {} schedules to mark as overdue", overdueSchedules.size());

        int processed = 0;
        for (RepaymentSchedule schedule : overdueSchedules) {
            try {
                processOverdueSchedule(schedule, today);
                processed++;
            } catch (Exception e) {
                log.error("Error processing schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }

        log.info("Overdue processing completed: {}/{} schedules processed", processed, overdueSchedules.size());
    }

    @Scheduled(cron = "${app.scheduler.dpd-update:0 30 1 * * ?}")
    @Transactional
    public void updateLoanDpd() {
        log.info("Starting DPD update job");
        LocalDate today = LocalDate.now();

        List<Loan> activeLoans = loanRepository.findAllActiveLoans();
        log.info("Updating DPD for {} active loans", activeLoans.size());

        for (Loan loan : activeLoans) {
            try {
                updateLoanDpdStatus(loan, today);
            } catch (Exception e) {
                log.error("Error updating DPD for loan {}: {}", loan.getId(), e.getMessage());
            }
        }

        log.info("DPD update job completed");
    }

    private void processOverdueSchedule(RepaymentSchedule schedule, LocalDate today) {
        if (schedule.getStatus() == ScheduleStatus.PAID) {
            return;
        }

        long daysPastDue = ChronoUnit.DAYS.between(schedule.getDueDate(), today);

        if (daysPastDue > 0) {
            schedule.setStatus(ScheduleStatus.OVERDUE);
            schedule.setDaysPastDue((int) daysPastDue);

            Loan loan = loanRepository.findById(schedule.getLoanId()).orElse(null);
            if (loan != null && loan.getPenaltyRate() != null && loan.getPenaltyRate().compareTo(BigDecimal.ZERO) > 0) {
                if (schedule.getPenaltyCalculatedAt() == null ||
                    !schedule.getPenaltyCalculatedAt().toString().startsWith(today.toString())) {

                    BigDecimal penaltyAmount = calculatePenalty(schedule, loan);
                    schedule.setPenaltyAmount(schedule.getPenaltyAmount().add(penaltyAmount));
                    schedule.setPenaltyCalculatedAt(java.time.Instant.now());

                    loan.setOutstandingPenalty(loan.getOutstandingPenalty().add(penaltyAmount));
                    loanRepository.save(loan);
                }
            }

            scheduleRepository.save(schedule);
            log.debug("Marked schedule {} as overdue, DPD: {}", schedule.getId(), daysPastDue);
        }
    }

    private void updateLoanDpdStatus(Loan loan, LocalDate today) {
        List<RepaymentSchedule> overdueSchedules = scheduleRepository
                .findByLoanIdAndStatusIn(loan.getId(), List.of(ScheduleStatus.OVERDUE, ScheduleStatus.PARTIAL));

        if (overdueSchedules.isEmpty()) {
            loan.setDpd(0);
            loan.setDpdBucket(null);
        } else {
            LocalDate earliestOverdue = overdueSchedules.stream()
                    .map(RepaymentSchedule::getDueDate)
                    .min(LocalDate::compareTo)
                    .orElse(today);

            int dpd = (int) ChronoUnit.DAYS.between(earliestOverdue, today);
            loan.setDpd(Math.max(dpd, 0));
            loan.setDpdBucket(calculateDpdBucket(loan.getDpd()));

            if (loan.getDpd() >= 90 && !Boolean.TRUE.equals(loan.getIsNpa())) {
                loan.setIsNpa(true);
                loan.setNpaDate(today);
                log.info("Loan {} marked as NPA with DPD {}", loan.getId(), loan.getDpd());
            }
        }

        loanRepository.save(loan);
    }

    private BigDecimal calculatePenalty(RepaymentSchedule schedule, Loan loan) {
        BigDecimal outstandingAmount = schedule.getOutstandingAmount();
        BigDecimal dailyPenaltyRate = loan.getPenaltyRate().divide(BigDecimal.valueOf(36500), 10, RoundingMode.HALF_UP);
        return outstandingAmount.multiply(dailyPenaltyRate).setScale(2, RoundingMode.HALF_UP);
    }

    private String calculateDpdBucket(int dpd) {
        if (dpd == 0) return null;
        if (dpd <= 7) return "1-7";
        if (dpd <= 30) return "8-30";
        if (dpd <= 60) return "31-60";
        if (dpd <= 90) return "61-90";
        return "90+";
    }
}
