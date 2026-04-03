package com.example.dailymenu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 추천 병렬 조회 전용 스레드 풀 설정.
 * ForkJoinPool.commonPool() 대신 사용하여 JPA Session 획득이 가능한 환경에서 실행.
 *
 * CLAUDE.md §6: "병렬 구현 방식: CompletableFuture.allOf() 사용, ThreadPoolTaskExecutor 빈 별도 설정"
 * DB connection pool 과부하 방지: corePoolSize를 3으로 제한 (병렬 조회 3건 기준)
 */
@Configuration
public class AsyncConfig {

    @Bean("recommendationQueryExecutor")
    public Executor recommendationQueryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("rec-query-");
        executor.initialize();
        return executor;
    }
}
