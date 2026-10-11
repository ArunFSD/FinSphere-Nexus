package com.finsphere.frontend.client;

import java.util.List;

import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.AuctionResultResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.dto.chit.MemberResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Chit schemes, enrolments, auctions and the member directory of chit-finance-service. */
@HttpExchange(url = "/chits", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ChitClient {

    @GetExchange("/plans/active")
    ApiResponse<PageResponse<ChitPlanResponse>> activePlans(@RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetExchange("/plans/{planId}")
    ApiResponse<ChitPlanDetailResponse> plan(@PathVariable("planId") long planId);

    @PostExchange(url = "/plans/create", contentType = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<ChitPlanResponse> createPlan(@RequestBody ChitPlanRequest request);

    @PostExchange(url = "/enrollments/join", contentType = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<ChitEnrollmentResponse> enroll(@RequestBody EnrollmentRequest request);

    @GetExchange("/enrollments/me")
    ApiResponse<List<ChitEnrollmentResponse>> myEnrollments();

    @GetExchange("/enrollments/user/{userId}")
    ApiResponse<List<ChitEnrollmentResponse>> enrollmentsOf(@PathVariable("userId") long userId);

    @PostExchange(url = "/cycles/auction", contentType = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<AuctionResultResponse> settleAuction(@RequestBody AuctionRequest request);

    @GetExchange("/members")
    ApiResponse<PageResponse<MemberResponse>> members(@RequestParam("page") int page,
            @RequestParam("size") int size);
}
