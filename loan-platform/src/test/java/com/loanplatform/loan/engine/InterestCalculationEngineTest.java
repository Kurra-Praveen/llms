package com.loanplatform.loan.engine;

import com.loanplatform.loan.entity.InterestType;
import com.loanplatform.loan.entity.RepaymentFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterestCalculationEngine Unit Tests")
class InterestCalculationEngineTest {

    private InterestCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new InterestCalculationEngine();
    }

    @Nested
    @DisplayName("Flat Interest Calculation Tests")
    class FlatInterestTests {

        @Test
        @DisplayName("Should calculate flat interest correctly")
        void shouldCalculateFlatInterestCorrectly() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.FLAT)
                    .tenureMonths(12)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            assertThat(schedule).hasSize(12);

            // Total interest for flat = Principal * Rate * Time = 100000 * 12% * 1 = 12000
            BigDecimal totalInterest = schedule.stream()
                    .map(ScheduleEntry::getInterestComponent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalInterest.setScale(0, RoundingMode.HALF_UP))
                    .isEqualByComparingTo(new BigDecimal("12000"));

            // Each installment should have same interest = 12000/12 = 1000
            assertThat(schedule.get(0).getInterestComponent().setScale(2, RoundingMode.HALF_UP))
                    .isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }

    @Nested
    @DisplayName("Reducing Balance Interest Calculation Tests")
    class ReducingBalanceTests {

        @Test
        @DisplayName("Should calculate reducing balance interest correctly")
        void shouldCalculateReducingBalanceInterestCorrectly() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            assertThat(schedule).hasSize(12);

            // EMI should be constant for reducing balance
            BigDecimal firstEmi = schedule.get(0).getInstallmentAmount();
            BigDecimal lastEmi = schedule.get(11).getInstallmentAmount();
            assertThat(firstEmi.subtract(lastEmi).abs())
                    .isLessThan(new BigDecimal("1")); // Allow small rounding difference

            // Interest should decrease over time
            BigDecimal firstInterest = schedule.get(0).getInterestComponent();
            BigDecimal lastInterest = schedule.get(11).getInterestComponent();
            assertThat(firstInterest).isGreaterThan(lastInterest);

            // Principal should increase over time
            BigDecimal firstPrincipal = schedule.get(0).getPrincipalComponent();
            BigDecimal lastPrincipal = schedule.get(11).getPrincipalComponent();
            assertThat(firstPrincipal).isLessThan(lastPrincipal);

            // Final outstanding should be zero (or very close)
            assertThat(schedule.get(11).getOutstandingAfter())
                    .isLessThan(new BigDecimal("1"));
        }

        @Test
        @DisplayName("Should calculate correct EMI for reducing balance")
        void shouldCalculateCorrectEmi() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            // Expected EMI ~= 8884.88 (calculated using standard EMI formula)
            BigDecimal emi = schedule.get(0).getInstallmentAmount();
            assertThat(emi).isBetween(new BigDecimal("8800"), new BigDecimal("8900"));
        }
    }

    @Nested
    @DisplayName("Simple Interest Calculation Tests")
    class SimpleInterestTests {

        @Test
        @DisplayName("Should calculate simple interest correctly")
        void shouldCalculateSimpleInterestCorrectly() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.SIMPLE)
                    .tenureMonths(12)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            assertThat(schedule).hasSize(12);

            // Simple interest = P * R * T = 100000 * 0.12 * 1 = 12000
            BigDecimal totalInterest = schedule.stream()
                    .map(ScheduleEntry::getInterestComponent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalInterest.setScale(0, RoundingMode.HALF_UP))
                    .isEqualByComparingTo(new BigDecimal("12000"));
        }
    }

    @Nested
    @DisplayName("Bullet Loan Tests")
    class BulletLoanTests {

        @Test
        @DisplayName("Should generate single payment for bullet loan")
        void shouldGenerateSinglePaymentForBulletLoan() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.BULLET)
                    .tenureMonths(12)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            // Bullet loan has one final payment
            assertThat(schedule).isNotEmpty();

            // Last entry should have principal payment
            ScheduleEntry lastEntry = schedule.get(schedule.size() - 1);
            assertThat(lastEntry.getPrincipalComponent())
                    .isEqualByComparingTo(new BigDecimal("100000"));
        }
    }

    @Nested
    @DisplayName("Interest Only Loan Tests")
    class InterestOnlyTests {

        @Test
        @DisplayName("Should pay only interest until final payment")
        void shouldPayOnlyInterestUntilFinalPayment() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.INTEREST_ONLY)
                    .tenureMonths(12)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            assertThat(schedule).hasSize(12);

            // First 11 payments should be interest only (no principal)
            for (int i = 0; i < 11; i++) {
                assertThat(schedule.get(i).getPrincipalComponent())
                        .isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(schedule.get(i).getInterestComponent())
                        .isEqualByComparingTo(new BigDecimal("1000")); // 100000 * 12% / 12
            }

            // Last payment should include full principal
            ScheduleEntry lastEntry = schedule.get(11);
            assertThat(lastEntry.getPrincipalComponent())
                    .isEqualByComparingTo(new BigDecimal("100000"));
        }
    }

    @Nested
    @DisplayName("Frequency Tests")
    class FrequencyTests {

        @ParameterizedTest
        @EnumSource(RepaymentFrequency.class)
        @DisplayName("Should generate correct number of installments for different frequencies")
        void shouldGenerateCorrectInstallmentsForFrequency(RepaymentFrequency frequency) {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(12)
                    .frequency(frequency)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            int expectedInstallments = switch (frequency) {
                case DAILY -> 365;
                case WEEKLY -> 52;
                case BI_WEEKLY -> 26;
                case MONTHLY -> 12;
                case QUARTERLY -> 4;
            };

            assertThat(schedule.size()).isCloseTo(expectedInstallments, org.assertj.core.data.Offset.offset(5));
        }
    }

    @Nested
    @DisplayName("Schedule Validation Tests")
    class ScheduleValidationTests {

        @Test
        @DisplayName("Should have correct due dates with monthly frequency")
        void shouldHaveCorrectDueDatesMonthly() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(3)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(startDate)
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            assertThat(schedule.get(0).getDueDate()).isEqualTo(LocalDate.of(2024, 2, 1));
            assertThat(schedule.get(1).getDueDate()).isEqualTo(LocalDate.of(2024, 3, 1));
            assertThat(schedule.get(2).getDueDate()).isEqualTo(LocalDate.of(2024, 4, 1));
        }

        @Test
        @DisplayName("Should respect grace period")
        void shouldRespectGracePeriod() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(1)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(startDate)
                    .gracePeriodDays(15)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            // First due date should be start + grace period + 1 month
            assertThat(schedule.get(0).getDueDate()).isEqualTo(LocalDate.of(2024, 2, 16));
        }

        @Test
        @DisplayName("Should have sequential installment numbers")
        void shouldHaveSequentialInstallmentNumbers() {
            ScheduleGenerationRequest request = ScheduleGenerationRequest.builder()
                    .principal(new BigDecimal("100000"))
                    .annualInterestRate(new BigDecimal("12"))
                    .interestType(InterestType.REDUCING_BALANCE)
                    .tenureMonths(6)
                    .frequency(RepaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .gracePeriodDays(0)
                    .build();

            List<ScheduleEntry> schedule = engine.generateSchedule(request);

            for (int i = 0; i < schedule.size(); i++) {
                assertThat(schedule.get(i).getInstallmentNumber()).isEqualTo(i + 1);
            }
        }
    }
}
