package com.example.dailymenu;

import java.time.Duration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    static MySQLContainer<?> mysql =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0")) // 변경된 부분: 테스트용 MySQL 컨테이너 사용
                    .withDatabaseName("dailymenu")
                    .withUsername("test")
                    .withPassword("1234")
                    .withStartupTimeout(Duration.ofMinutes(2));

    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7")) // 변경된 부분: 테스트용 Redis 컨테이너 사용
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort())
                    .withStartupTimeout(Duration.ofMinutes(2));

    static {
        mysql.start(); // 변경된 부분: 테스트 시작 시 MySQL 컨테이너 실행
        redis.start(); // 변경된 부분: 테스트 시작 시 Redis 컨테이너 실행

        System.out.println("=== Testcontainers started ===");
        System.out.println("MySQL JDBC URL = " + mysql.getJdbcUrl());
        System.out.println("Redis Host = " + redis.getHost());
        System.out.println("Redis Port = " + redis.getMappedPort(6379));
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        System.out.println("=== registerProperties 실행됨 ===");

        registry.add("spring.datasource.url", mysql::getJdbcUrl); // 변경된 부분: MySQL 동적 URL 주입
        registry.add("spring.datasource.username", mysql::getUsername); // 변경된 부분: MySQL username 주입
        registry.add("spring.datasource.password", mysql::getPassword); // 변경된 부분: MySQL password 주입
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver"); // 변경된 부분: MySQL driver 지정

        registry.add("spring.data.redis.host", redis::getHost); // 변경된 부분: Redis host 주입
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379)); // 변경된 부분: Redis port 주입
    }
}