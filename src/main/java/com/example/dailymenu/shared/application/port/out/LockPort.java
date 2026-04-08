package com.example.dailymenu.shared.application.port.out;

/**
 * 분산 락 Port.
 * 락은 비즈니스 규칙이 아닌 실행 흐름 제어다 — Domain이 아닌 Application이 정의한다.
 * RedisLockAdapter가 구현한다.
 *
 * 사용 원칙 (CLAUDE.md 동시성 제어 규칙):
 *   - tryLock: 트랜잭션 시작 전에 호출
 *   - unlock: 트랜잭션 커밋 이후에 호출 (Facade의 finally 블록)
 *   - TTL: 5초 고정
 */
public interface LockPort {

    /**
     * 분산 락 획득 시도.
     * @param key        락 키 (예: "recommendation:lock:{userId}")
     * @param ttlSeconds 락 만료 시간 (초)
     * @return 획득 성공 시 true, 이미 점유 중이면 false
     */
    boolean tryLock(String key, long ttlSeconds);

    /**
     * 분산 락 해제.
     * 반드시 트랜잭션 커밋 이후에 호출해야 한다.
     * @param key 락 키
     */
    void unlock(String key);
}
