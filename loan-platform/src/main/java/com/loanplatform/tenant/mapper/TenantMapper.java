package com.loanplatform.tenant.mapper;

import com.loanplatform.tenant.dto.CreateTenantRequest;
import com.loanplatform.tenant.dto.TenantResponse;
import com.loanplatform.tenant.entity.Tenant;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Tenant toEntity(CreateTenantRequest request);

    @Mapping(target = "status", expression = "java(tenant.getStatus().name())")
    @Mapping(target = "borrowerCount", ignore = true)
    @Mapping(target = "loanCount", ignore = true)
    @Mapping(target = "userCount", ignore = true)
    TenantResponse toResponse(Tenant tenant);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Tenant tenant, com.loanplatform.tenant.dto.UpdateTenantRequest request);
}
