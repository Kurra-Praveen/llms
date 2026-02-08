package com.loanplatform.loan.repository;

import com.loanplatform.loan.entity.RepaymentSchedule;
import com.loanplatform.loan.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loanId = :loanId ORDER BY rs.installmentNumber")
    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumber(@Param("loanId") UUID loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loanId = :loanId AND rs.status IN :statuses ORDER BY rs.installmentNumber")
    List<RepaymentSchedule> findByLoanIdAndStatusIn(@Param("loanId") UUID loanId, @Param("statuses") List<ScheduleStatus> statuses);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loanId = :loanId AND rs.status = 'PENDING' ORDER BY rs.installmentNumber")
    List<RepaymentSchedule> findPendingSchedulesByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loanId = :loanId AND rs.status IN ('PENDING', 'PARTIAL', 'OVERDUE') ORDER BY rs.dueDate, rs.installmentNumber")
    List<RepaymentSchedule> findUnpaidSchedulesByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.tenantId = :tenantId AND rs.dueDate <= :date AND rs.status IN ('PENDING', 'PARTIAL') ORDER BY rs.dueDate")
    List<RepaymentSchedule> findDueSchedulesForTenant(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.status = 'PENDING' AND rs.dueDate < :date")
    List<RepaymentSchedule> findOverdueSchedules(@Param("date") LocalDate date);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loanId = :loanId AND rs.installmentNumber = :installmentNumber")
    Optional<RepaymentSchedule> findByLoanIdAndInstallmentNumber(@Param("loanId") UUID loanId, @Param("installmentNumber") int installmentNumber);

    @Query("SELECT COUNT(rs) FROM RepaymentSchedule rs WHERE rs.loanId = :loanId AND rs.status = 'OVERDUE'")
    long countOverdueByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.tenantId = :tenantId AND rs.dueDate BETWEEN :startDate AND :endDate ORDER BY rs.dueDate")
    List<RepaymentSchedule> findSchedulesDueBetween(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
