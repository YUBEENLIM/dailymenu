package com.example.dailymenu.user.adapter.out.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Refresh Token SHA-256 해시 유틸.
 * 저장소(DB/Redis)에는 해시만 저장 — 저장소 탈취 시 원본 토큰 복구 불가.
 * 비교는 {@link MessageDigest#isEqual}로 상수 시간 수행(타이밍 공격 방지).
 */
@Component
public class TokenHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final HexFormat HEX = HexFormat.of();

    public String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 JDK 기본 Provider에 상주 — 도달 시 JVM 구성 이상.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public boolean matches(String raw, String storedHash) {
        if (raw == null || storedHash == null) {
            return false;
        }
        byte[] rawHash = hash(raw).getBytes(StandardCharsets.UTF_8);
        byte[] expected = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(rawHash, expected);
    }
}
