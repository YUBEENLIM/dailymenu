package com.example.dailymenu.user.adapter.in.web;

import com.example.dailymenu.user.adapter.in.web.dto.*;
import com.example.dailymenu.user.application.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 Controller — 변환과 위임만 담당 (api-spec.md §5).
 * 현재 이메일/비밀번호 인증. 추후 카카오 소셜 로그인(POST /auth/kakao) 추가 예정.
 * 응답은 ApiResponseWrappingAdvice가 자동으로 ApiResponse로 래핑한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @RequestBody @Valid RegisterRequest request
    ) {
        Long userId = authUseCase.register(
                request.email(), request.password(), request.nickname());
        return new RegisterResponse(userId);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody @Valid LoginRequest request
    ) {
        AuthUseCase.LoginResult result = authUseCase.login(
                request.email(), request.password());
        return new LoginResponse(result.accessToken(), result.refreshToken(), result.expiresIn());
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(
            @RequestBody @Valid RefreshRequest request
    ) {
        AuthUseCase.RefreshResult result = authUseCase.refresh(request.refreshToken());
        return new RefreshResponse(result.accessToken(), result.expiresIn());
    }

    @PostMapping("/logout")
    public void logout(
            @RequestAttribute("userId") Long userId
    ) {
        authUseCase.logout(userId);
    }
}
