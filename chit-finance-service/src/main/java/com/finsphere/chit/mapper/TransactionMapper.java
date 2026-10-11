package com.finsphere.chit.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.finsphere.chit.domain.PaymentTransaction;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionResponse;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface TransactionMapper {

    @Mapping(target = "correlationId", ignore = true)
    PaymentTransaction toDocument(TransactionRequest request);

    /**
     * @param transaction the payment
     * @param memberName  payer name from the user directory mirror, {@code null} when not known yet
     */
    @Mapping(target = "memberName", source = "memberName")
    TransactionResponse toResponse(PaymentTransaction transaction, @Nullable String memberName);
}
