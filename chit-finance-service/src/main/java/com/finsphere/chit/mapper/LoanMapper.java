package com.finsphere.chit.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.finsphere.chit.domain.Loan;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface LoanMapper {

    /** Derived amounts and dates are computed by {@code LoanScheduleCalculator} in the service. */
    @Mapping(target = "disbursedAmount", ignore = true)
    @Mapping(target = "dailyInstallment", ignore = true)
    @Mapping(target = "amountGivenDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "active", constant = "true")
    Loan toEntity(LoanRequest request);

    /**
     * @param loan       the loan
     * @param memberName borrower name from the user directory mirror, {@code null} when not known yet
     */
    @Mapping(target = "memberName", source = "memberName")
    LoanResponse toResponse(Loan loan, @Nullable String memberName);
}
