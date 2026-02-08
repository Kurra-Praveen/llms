package com.loanplatform.auth.repository;

import com.loanplatform.auth.entity.User;
import com.loanplatform.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.tenantId = :tenantId AND u.deleted = false")
    Optional<User> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deleted = false")
    Page<User> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.status = :status AND u.deleted = false")
    Page<User> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") UserStatus status, Pageable pageable);

    boolean existsByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenantId IS NULL AND u.email = :email AND u.deleted = false")
    Optional<User> findSuperAdminByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.deleted = false")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
