package com.loanplatform.collection.repository;

import com.loanplatform.collection.entity.CollectionActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollectionActivityRepository extends JpaRepository<CollectionActivity, UUID> {

    @Query("SELECT a FROM CollectionActivity a WHERE a.collectionCaseId = :caseId ORDER BY a.activityDate DESC")
    List<CollectionActivity> findByCollectionCaseId(@Param("caseId") UUID caseId);

    @Query("SELECT a FROM CollectionActivity a WHERE a.collectionCaseId = :caseId ORDER BY a.activityDate DESC")
    Page<CollectionActivity> findByCollectionCaseIdPaged(@Param("caseId") UUID caseId, Pageable pageable);

    @Query("SELECT a FROM CollectionActivity a WHERE a.tenantId = :tenantId AND a.createdBy = :userId ORDER BY a.activityDate DESC")
    Page<CollectionActivity> findByTenantIdAndCreatedBy(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            Pageable pageable);
}
