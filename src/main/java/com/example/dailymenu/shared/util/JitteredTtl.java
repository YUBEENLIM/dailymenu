package com.example.dailymenu.shared.util;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 캐시 TTL에 ±ratio 비율의 Jitter를 적용하는 정적 유틸.
 * 동일 시각 일괄 만료로 인한 cache stampede를 시간축에 분산시킨다.
 *
 * 단독으로는 완전한 스탬피드 방어가 되지 못하며, SET NX 기반 single-flight와 함께 사용한다.
 * 예: of(Duration.ofHours(1), 0.1) → 3240s ~ 3960s 균일 분포.
 */
public final class JitteredTtl {

    private JitteredTtl() {}

    /**
     * @param base        기본 TTL
     * @param jitterRatio 0~1 범위. 0.1이면 base의 ±10%를 균일 분포로 가산.
     *                    음수/0 이하면 base 그대로 반환. 1 초과는 1로 clamp (TTL이 음수로 떨어지는 것 방지).
     * @return base ± (base * jitterRatio) 범위의 Duration. 최소 1초 보장.
     */
    public static Duration of(Duration base, double jitterRatio) {
        if (jitterRatio <= 0) {
            return base;
        }
        double ratio = Math.min(jitterRatio, 1.0);
        long baseSec = base.toSeconds();
        long jitter = (long) (baseSec * ratio);
        if (jitter <= 0) {
            return base;
        }
        long delta = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        long finalSec = Math.max(1, baseSec + delta);
        return Duration.ofSeconds(finalSec);
    }
}
