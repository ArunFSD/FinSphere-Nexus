package com.finsphere.chit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.finsphere.chit.service.UserDirectoryService;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.MemberResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.MEMBERS)
@Tag(name = "Members")
public class MemberController {

    private final UserDirectoryService directory;

    public MemberController(UserDirectoryService directory) {
        this.directory = directory;
    }

    @Operation(summary = "Active members known to this service (admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> members(
            @PageableDefault(size = 50, sort = "fullName") Pageable pageable) {
        return ApiResponse.ok("Members", this.directory.activeMembers(pageable)).toResponseEntity();
    }
}
