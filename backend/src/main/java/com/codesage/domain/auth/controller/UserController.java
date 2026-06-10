package com.codesage.domain.auth.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.auth.dto.response.UserResponseDto;
import com.codesage.domain.auth.service.UserService;
import com.codesage.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "User profile operations")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get Current User", description = "Retrieves the profile of the currently authenticated user")
    @GetMapping("/me")
    public ApiResponse<UserResponseDto> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        UserResponseDto userProfile = userService.findById(userPrincipal.getId());
        return ApiResponse.success("Profile retrieved", userProfile);
    }
}
