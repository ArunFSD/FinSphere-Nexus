package com.finsphere.frontend.client;

import java.util.List;

import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Loans and the payment log book of chit-finance-service. */
@HttpExchange(url = "/finance", accept = MediaType.APPLICATION_JSON_VALUE)
public interface FinanceClient {

    @PostExchange(url = "/loans/disburse", contentType = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<LoanResponse> disburse(@RequestBody LoanRequest request);

    @GetExchange("/loans")
    ApiResponse<PageResponse<LoanResponse>> loans(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetExchange("/loans/me")
    ApiResponse<List<LoanResponse>> myLoans();

    @GetExchange("/loans/user/{userId}")
    ApiResponse<List<LoanResponse>> loansOf(@PathVariable("userId") long userId);

    @PostExchange(url = "/transactions/log", contentType = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<TransactionResponse> recordPayment(@RequestBody TransactionRequest request);

    @GetExchange("/transactions")
    ApiResponse<PageResponse<TransactionResponse>> payments(@RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetExchange("/transactions/me")
    ApiResponse<PageResponse<TransactionResponse>> myPayments(@RequestParam("page") int page,
            @RequestParam("size") int size);
}
