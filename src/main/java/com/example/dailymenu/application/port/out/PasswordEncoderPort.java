package com.example.dailymenu.application.port.out;

/**
 * 비밀번호 해싱 Port.
 * BcryptPasswordAdapter 가 구현.
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
