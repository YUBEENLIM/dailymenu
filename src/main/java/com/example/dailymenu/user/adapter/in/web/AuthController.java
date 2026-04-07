package com.example.dailymenu.user.adapter.in.web;

import com.example.dailymenu.user.adapter.in.web.dto.*;
import com.example.dailymenu.user.application.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 인증 Controller — 변환과 위임만 담당 (api-spec.md §5).
 * 일반 로그인(이메일/비밀번호) + 카카오 OAuth 로그인 지원.
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

    @PostMapping("/kakao")
    public LoginResponse kakaoLogin(
            @RequestBody @Valid KakaoLoginRequest request
    ) {
        AuthUseCase.LoginResult result = authUseCase.kakaoLogin(request.code());
        return new LoginResponse(result.accessToken(), result.refreshToken(), result.expiresIn());
    }

    /**
     * 카카오 OAuth 콜백 — 카카오에서 인가 코드와 함께 리다이렉트되는 엔드포인트.
     * 서버에서 바로 토큰 교환 후 JWT를 쿼리 파라미터로 프론트에 전달.
     */
    @GetMapping("/kakao/callback")
    public RedirectView kakaoCallback(@RequestParam String code) {
        AuthUseCase.LoginResult result = authUseCase.kakaoLogin(code);
        return new RedirectView("/index.html?accessToken=" + result.accessToken()
                + "&refreshToken=" + result.refreshToken()
                + "&expiresIn=" + result.expiresIn());
    }

    @PostMapping("/logout")
    public void logout(
            @RequestAttribute("userId") Long userId
    ) {
        authUseCase.logout(userId);
    }
}
