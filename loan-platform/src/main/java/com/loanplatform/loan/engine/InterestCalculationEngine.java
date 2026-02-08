package com.loanplatform.loan.engine;

import com.loanplatform.loan.entity.InterestType;
import com.loanplatform.loan.entity.RepaymentFrequency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class InterestCalculationEngine {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

    public List<ScheduleEntry> generateSchedule(ScheduleGenerationRequest request) {
        log.debug("Generating schedule for principal: {}, rate: {}, type: {}",
                request.getPrincipal(), request.getAnnualInterestRate(), request.getInterestType());

        return switch (request.getInterestType()) {
            case FLAT -> generateFlatSchedule(request);
            case REDUCING_BALANCE -> generateReducingBalanceSchedule(request);
            case SIMPLE -> generateSimpleInterestSchedule(request);
            case INTEREST_ONLY -> generateInterestOnlySchedule(request);
            case BULLET -> generateBulletSchedule(request);
            case DAILY_FIXED -> generateDailyFixedSchedule(request);
        };
    }

    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths, InterestType type) {
        return calculateEmi(principal, annualRate, tenureMonths, type, null);
    }

    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths, InterestType type, BigDecimal dailyFixedAmount) {
        return switch (type) {
            case FLAT -> calculateFlatEmi(principal, annualRate, tenureMonths);
            case REDUCING_BALANCE -> calculateReducingBalanceEmi(principal, annualRate, tenureMonths);
            case SIMPLE -> calculateSimpleEmi(principal, annualRate, tenureMonths);
            case INTEREST_ONLY -> calculateInterestOnlyPayment(principal, annualRate);
            case BULLET -> BigDecimal.ZERO;
            case DAILY_FIXED -> calculateDailyFixedEmi(principal, dailyFixedAmount, tenureMonths);
        };
    }

    public BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal annualRate, int tenureMonths, InterestType type) {
        return calculateTotalInterest(principal, annualRate, tenureMonths, type, null);
    }

    public BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal annualRate, int tenureMonths, InterestType type, BigDecimal dailyFixedAmount) {
        return switch (type) {
            case FLAT -> calculateFlatTotalInterest(principal, annualRate, tenureMonths);
            case REDUCING_BALANCE -> calculateReducingBalanceTotalInterest(principal, annualRate, tenureMonths);
            case SIMPLE -> calculateSimpleTotalInterest(principal, annualRate, tenureMonths);
            case INTEREST_ONLY -> calculateInterestOnlyTotalInterest(principal, annualRate, tenureMonths);
            case BULLET -> calculateSimpleTotalInterest(principal, annualRate, tenureMonths);
            case DAILY_FIXED -> calculateDailyFixedTotalInterest(principal, dailyFixedAmount, tenureMonths);
        };
    }

    private List<ScheduleEntry> generateFlatSchedule(ScheduleGenerationRequest request) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        BigDecimal monthlyRate = request.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), MC);
        int numInstallments = calculateNumberOfInstallments(request.getTenureMonths(), request.getFrequency());

        BigDecimal totalInterest = principal.multiply(monthlyRate)
                .multiply(BigDecimal.valueOf(request.getTenureMonths()));
        BigDecimal totalPayable = principal.add(totalInterest);
        BigDecimal emi = totalPayable.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);

        BigDecimal principalPerInstallment = principal.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);
        BigDecimal interestPerInstallment = totalInterest.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);

        BigDecimal outstanding = principal;
        LocalDate dueDate = calculateFirstDueDate(request.getStartDate(), request.getFrequency(), request.getGracePeriodDays());

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal principalComp = (i == numInstallments)
                    ? outstanding
                    : principalPerInstallment;

            outstanding = outstanding.subtract(principalComp);
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }

            schedule.add(ScheduleEntry.builder()
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalComponent(principalComp.setScale(SCALE, RoundingMode.HALF_UP))
                    .interestComponent(interestPerInstallment)
                    .installmentAmount(principalComp.add(interestPerInstallment).setScale(SCALE, RoundingMode.HALF_UP))
                    .outstandingAfter(outstanding.setScale(SCALE, RoundingMode.HALF_UP))
                    .build());

            dueDate = advanceDate(dueDate, request.getFrequency());
        }

        return schedule;
    }

    private List<ScheduleEntry> generateReducingBalanceSchedule(ScheduleGenerationRequest request) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        BigDecimal monthlyRate = request.getAnnualInterestRate().divide(BigDecimal.valueOf(1200), MC);
        int numInstallments = calculateNumberOfInstallments(request.getTenureMonths(), request.getFrequency());

        BigDecimal periodRate = adjustRateForFrequency(monthlyRate, request.getFrequency());

        BigDecimal emi = calculateReducingBalanceEmi(principal, request.getAnnualInterestRate(), numInstallments);

        BigDecimal outstanding = principal;
        LocalDate dueDate = calculateFirstDueDate(request.getStartDate(), request.getFrequency(), request.getGracePeriodDays());

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal interestComp = outstanding.multiply(periodRate).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal principalComp = emi.subtract(interestComp);

            if (i == numInstallments) {
                principalComp = outstanding;
                emi = principalComp.add(interestComp);
            }

            outstanding = outstanding.subtract(principalComp);
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }

            schedule.add(ScheduleEntry.builder()
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalComponent(principalComp.setScale(SCALE, RoundingMode.HALF_UP))
                    .interestComponent(interestComp)
                    .installmentAmount(emi.setScale(SCALE, RoundingMode.HALF_UP))
                    .outstandingAfter(outstanding.setScale(SCALE, RoundingMode.HALF_UP))
                    .build());

            dueDate = advanceDate(dueDate, request.getFrequency());
        }

        return schedule;
    }

    private List<ScheduleEntry> generateSimpleInterestSchedule(ScheduleGenerationRequest request) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        BigDecimal totalInterest = calculateSimpleTotalInterest(principal, request.getAnnualInterestRate(), request.getTenureMonths());
        int numInstallments = calculateNumberOfInstallments(request.getTenureMonths(), request.getFrequency());

        BigDecimal principalPerInstallment = principal.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);
        BigDecimal interestPerInstallment = totalInterest.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);

        BigDecimal outstanding = principal;
        LocalDate dueDate = calculateFirstDueDate(request.getStartDate(), request.getFrequency(), request.getGracePeriodDays());

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal principalComp = (i == numInstallments) ? outstanding : principalPerInstallment;
            outstanding = outstanding.subtract(principalComp);
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }

            schedule.add(ScheduleEntry.builder()
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalComponent(principalComp.setScale(SCALE, RoundingMode.HALF_UP))
                    .interestComponent(interestPerInstallment)
                    .installmentAmount(principalComp.add(interestPerInstallment).setScale(SCALE, RoundingMode.HALF_UP))
                    .outstandingAfter(outstanding.setScale(SCALE, RoundingMode.HALF_UP))
                    .build());

            dueDate = advanceDate(dueDate, request.getFrequency());
        }

        return schedule;
    }

    private List<ScheduleEntry> generateInterestOnlySchedule(ScheduleGenerationRequest request) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        BigDecimal monthlyInterest = calculateInterestOnlyPayment(principal, request.getAnnualInterestRate());
        int numInstallments = calculateNumberOfInstallments(request.getTenureMonths(), request.getFrequency());

        LocalDate dueDate = calculateFirstDueDate(request.getStartDate(), request.getFrequency(), request.getGracePeriodDays());

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal principalComp = (i == numInstallments) ? principal : BigDecimal.ZERO;
            BigDecimal outstanding = (i == numInstallments) ? BigDecimal.ZERO : principal;

            schedule.add(ScheduleEntry.builder()
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalComponent(principalComp.setScale(SCALE, RoundingMode.HALF_UP))
                    .interestComponent(monthlyInterest)
                    .installmentAmount(principalComp.add(monthlyInterest).setScale(SCALE, RoundingMode.HALF_UP))
                    .outstandingAfter(outstanding.setScale(SCALE, RoundingMode.HALF_UP))
                    .build());

            dueDate = advanceDate(dueDate, request.getFrequency());
        }

        return schedule;
    }

    private List<ScheduleEntry> generateBulletSchedule(ScheduleGenerationRequest request) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        BigDecimal totalInterest = calculateSimpleTotalInterest(principal, request.getAnnualInterestRate(), request.getTenureMonths());

        LocalDate dueDate = request.getStartDate().plusMonths(request.getTenureMonths());

        schedule.add(ScheduleEntry.builder()
                .installmentNumber(1)
                .dueDate(dueDate)
                .principalComponent(principal)
                .interestComponent(totalInterest.setScale(SCALE, RoundingMode.HALF_UP))
                .installmentAmount(principal.add(totalInterest).setScale(SCALE, RoundingMode.HALF_UP))
                .outstandingAfter(BigDecimal.ZERO)
                .build());

        return schedule;
    }

    /**
     * Generate schedule for DAILY_FIXED interest type.
     * Fixed rupee amount per ₹100 of principal per day.
     * Daily Interest = (Principal / 100) × Daily Fixed Rate
     * Total Interest = Daily Interest × Total Days in tenure
     */
    private List<ScheduleEntry> generateDailyFixedSchedule(ScheduleGenerationRequest request) {
        List<ScheduleEntry> schedule = new ArrayList<>();

        BigDecimal principal = request.getPrincipal();
        BigDecimal dailyFixedRate = request.getDailyFixedAmount() != null
                ? request.getDailyFixedAmount()
                : BigDecimal.ZERO;

        int numInstallments = calculateNumberOfInstallments(request.getTenureMonths(), request.getFrequency());
        int totalDays = request.getTenureMonths() * 30; // Approximate days in tenure

        // Daily Interest = (Principal / 100) × Daily Fixed Rate
        // Total Interest = Daily Interest × Total Days
        BigDecimal dailyInterest = principal.divide(BigDecimal.valueOf(100), MC).multiply(dailyFixedRate);
        BigDecimal totalInterest = dailyInterest.multiply(BigDecimal.valueOf(totalDays));
        BigDecimal interestPerInstallment = totalInterest.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);
        BigDecimal principalPerInstallment = principal.divide(BigDecimal.valueOf(numInstallments), SCALE, RoundingMode.HALF_UP);

        BigDecimal outstanding = principal;
        LocalDate dueDate = calculateFirstDueDate(request.getStartDate(), request.getFrequency(), request.getGracePeriodDays());

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal principalComp = (i == numInstallments) ? outstanding : principalPerInstallment;
            outstanding = outstanding.subtract(principalComp);
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }

            schedule.add(ScheduleEntry.builder()
                    .installmentNumber(i)
                    .dueDate(dueDate)
                    .principalComponent(principalComp.setScale(SCALE, RoundingMode.HALF_UP))
                    .interestComponent(interestPerInstallment)
                    .installmentAmount(principalComp.add(interestPerInstallment).setScale(SCALE, RoundingMode.HALF_UP))
                    .outstandingAfter(outstanding.setScale(SCALE, RoundingMode.HALF_UP))
                    .build());

            dueDate = advanceDate(dueDate, request.getFrequency());
        }

        return schedule;
    }

    private BigDecimal calculateFlatEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal totalInterest = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(tenureMonths));
        BigDecimal totalPayable = principal.add(totalInterest);
        return totalPayable.divide(BigDecimal.valueOf(tenureMonths), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateReducingBalanceEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenureMonths, MC);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSimpleEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        BigDecimal totalInterest = calculateSimpleTotalInterest(principal, annualRate, tenureMonths);
        BigDecimal totalPayable = principal.add(totalInterest);
        return totalPayable.divide(BigDecimal.valueOf(tenureMonths), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateInterestOnlyPayment(BigDecimal principal, BigDecimal annualRate) {
        return principal.multiply(annualRate)
                .divide(BigDecimal.valueOf(1200), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFlatTotalInterest(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), MC);
        return principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(tenureMonths))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateReducingBalanceTotalInterest(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        BigDecimal emi = calculateReducingBalanceEmi(principal, annualRate, tenureMonths);
        BigDecimal totalPayment = emi.multiply(BigDecimal.valueOf(tenureMonths));
        return totalPayment.subtract(principal).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSimpleTotalInterest(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        return principal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .divide(BigDecimal.valueOf(1200), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateInterestOnlyTotalInterest(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        return calculateInterestOnlyPayment(principal, annualRate)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate EMI for DAILY_FIXED interest type.
     * Daily Interest = (Principal / 100) × Daily Fixed Rate
     * Total Interest = Daily Interest × Total Days
     * EMI = (Principal + Total Interest) / Number of Installments
     */
    private BigDecimal calculateDailyFixedEmi(BigDecimal principal, BigDecimal dailyFixedRate, int tenureMonths) {
        if (dailyFixedRate == null || dailyFixedRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal totalInterest = calculateDailyFixedTotalInterest(principal, dailyFixedRate, tenureMonths);
        BigDecimal totalPayable = principal.add(totalInterest);
        return totalPayable.divide(BigDecimal.valueOf(tenureMonths), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate total interest for DAILY_FIXED type.
     * Daily Interest = (Principal / 100) × Daily Fixed Rate
     * Total Interest = Daily Interest × Total Days (approx 30 days/month)
     */
    private BigDecimal calculateDailyFixedTotalInterest(BigDecimal principal, BigDecimal dailyFixedRate, int tenureMonths) {
        if (dailyFixedRate == null || principal == null) {
            return BigDecimal.ZERO;
        }
        int totalDays = tenureMonths * 30; // Approximate days
        // Daily Interest = (Principal / 100) × Daily Fixed Rate
        BigDecimal dailyInterest = principal.divide(BigDecimal.valueOf(100), MC).multiply(dailyFixedRate);
        return dailyInterest.multiply(BigDecimal.valueOf(totalDays))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private int calculateNumberOfInstallments(int tenureMonths, RepaymentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> tenureMonths * 30;
            case WEEKLY -> tenureMonths * 4;
            case BI_WEEKLY -> tenureMonths * 2;
            case MONTHLY -> tenureMonths;
            case QUARTERLY -> tenureMonths / 3;
        };
    }

    private BigDecimal adjustRateForFrequency(BigDecimal monthlyRate, RepaymentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> monthlyRate.divide(BigDecimal.valueOf(30), MC);
            case WEEKLY -> monthlyRate.divide(BigDecimal.valueOf(4), MC);
            case BI_WEEKLY -> monthlyRate.divide(BigDecimal.valueOf(2), MC);
            case MONTHLY -> monthlyRate;
            case QUARTERLY -> monthlyRate.multiply(BigDecimal.valueOf(3));
        };
    }

    private LocalDate calculateFirstDueDate(LocalDate startDate, RepaymentFrequency frequency, int gracePeriodDays) {
        LocalDate firstDue = startDate.plusDays(gracePeriodDays);
        return advanceDate(firstDue, frequency);
    }

    private LocalDate advanceDate(LocalDate date, RepaymentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case BI_WEEKLY -> date.plusWeeks(2);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
        };
    }
}
