package com.loanplatform.collection.mapper;

import com.loanplatform.collection.dto.CollectionActivityResponse;
import com.loanplatform.collection.dto.CollectionCaseResponse;
import com.loanplatform.collection.entity.CollectionActivity;
import com.loanplatform.collection.entity.CollectionCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CollectionMapper {

    @Mapping(target = "loanNumber", source = "loan.loanNumber")
    @Mapping(target = "borrowerName", source = "loan.borrower.fullName")
    @Mapping(target = "borrowerPhone", source = "loan.borrower.phone")
    @Mapping(target = "assignedToName", source = "assignedUser.fullName")
    @Mapping(target = "resolvedByName", ignore = true)
    @Mapping(target = "activities", ignore = true)
    CollectionCaseResponse toResponse(CollectionCase collectionCase);

    @Mapping(target = "createdByName", ignore = true)
    CollectionActivityResponse toActivityResponse(CollectionActivity activity);

    List<CollectionActivityResponse> toActivityResponses(List<CollectionActivity> activities);

    List<CollectionCaseResponse> toResponses(List<CollectionCase> cases);
}
