package com.finsphere.web.client;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.model.ChitPlanDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "chit-finance-service", url = "http://localhost:7080/chits/plans")
public interface SchemeFeignClient {

    @GetMapping("/active")
    ResponseEntity<ApiResponse<Page<ChitPlanDTO>>> getActivePlans(
            @RequestParam("page") int page,
            @RequestParam("size") int size);
}
