package com.codesage.domain.auth.controller;

import com.codesage.common.dto.ApiResponse;
import com.codesage.domain.auth.dto.request.GitHubCallbackRequestDto;
import com.codesage.domain.auth.dto.response.AuthTokenResponseDto;
import com.codesage.domain.auth.service.AuthService;
import com.codesage.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "GitHub OAuth and JWT operations")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Handle GitHub OAuth Callback", description = "Exchanges authorization code for tokens and creates session")
    @PostMapping("/github/callback")
    public ApiResponse<AuthTokenResponseDto> githubCallback(
            @Valid @RequestBody GitHubCallbackRequestDto requestDto,
            HttpServletResponse response) {
        
        AuthTokenResponseDto result = authService.handleGitHubCallback(requestDto, response);
        return ApiResponse.success("Authentication successful", result);
    }

    @Operation(summary = "Refresh Tokens", description = "Exchanges a valid refresh token for a new access/refresh token pair")
    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponseDto> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        AuthTokenResponseDto result = authService.refreshToken(request, response);
        return ApiResponse.success("Tokens refreshed", result);
    }

    @Operation(summary = "Logout", description = "Revokes current session and clears cookies")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        if (userPrincipal != null) {
            authService.logout(userPrincipal.getId(), request, response);
        }
        return ApiResponse.success("Logged out successfully");
    }
}
