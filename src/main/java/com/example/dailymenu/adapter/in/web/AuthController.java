package com.example.dailymenu.adapter.in.web;

import com.example.dailymenu.adapter.in.web.dto.auth.*;
import com.example.dailymenu.adapter.in.web.dto.common.ApiResponse;
import com.example.dailymenu.application.usecase.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 Controller — 변환과 위임만 담당 (api-spec.md §5).
 * 현재 이메일/비밀번호 인증. 추후 카카오 소셜 로그인(POST /auth/kakao) 추가 예정.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    /** POST /auth/register — 회원가입. 201 Created. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        Long userId = authUseCase.register(
                request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new RegisterResponse(userId)));
    }

    /** POST /auth/login — 로그인. Access Token 1시간, Refresh Token 7일. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        AuthUseCase.LoginResult result = authUseCase.login(
                request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(
                new LoginResponse(result.accessToken(), result.refreshToken(), result.expiresIn())));
    }

    /** POST /auth/refresh — Access Token 재발급. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @RequestBody @Valid RefreshRequest request
    ) {
        AuthUseCase.RefreshResult result = authUseCase.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(
                new RefreshResponse(result.accessToken(), result.expiresIn())));
    }

    /** POST /auth/logout — Refresh Token 무효화 (Redis 블랙리스트 등록). */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestAttribute("userId") Long userId
    ) {
        authUseCase.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
