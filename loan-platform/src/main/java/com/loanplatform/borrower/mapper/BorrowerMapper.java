package com.loanplatform.borrower.mapper;

import com.loanplatform.borrower.dto.BorrowerResponse;
import com.loanplatform.borrower.dto.CreateBorrowerRequest;
import com.loanplatform.borrower.dto.UpdateBorrowerRequest;
import com.loanplatform.borrower.entity.Borrower;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BorrowerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "borrowerCode", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "riskBand", constant = "UNRATED")
    @Mapping(target = "creditRating", constant = "UNRATED")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Borrower toEntity(CreateBorrowerRequest request);

    @Mapping(target = "status", expression = "java(borrower.getStatus().name())")
    @Mapping(target = "riskBand", expression = "java(borrower.getRiskBand().name())")
    @Mapping(target = "fullAddress", expression = "java(borrower.getFullAddress())")
    @Mapping(target = "activeLoansCount", ignore = true)
    @Mapping(target = "totalOutstanding", ignore = true)
    BorrowerResponse toResponse(Borrower borrower);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Borrower borrower, UpdateBorrowerRequest request);
}
