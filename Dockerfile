# 1단계: 애플리케이션 빌드용 이미지
FROM eclipse-temurin:17-jdk-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper와 빌드 설정 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew 실행 권한 부여# 변경된 부분: 리눅스 컨테이너에서 실행 가능하도록 권한 추가
RUN chmod +x ./gradlew

# 소스 코드 복사 # 변경된 부분: src 전체 복사
COPY src src

# 테스트를 포함해 빌드하려면 build, 테스트 제외면 bootJar 사용 가능, # 변경된 부분: 컨테이너 내부에서 jar 생성
RUN ./gradlew bootJar

# 2단계: 실행 전용 이미지 # 변경된 부분: 실행만 하므로 jre 이미지 사용
From eclipse-temurin:17-jre-jammy

# 컨테이너 내부 작업 디렉토리
WORKDIR /app

# builder 단계에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 변경된 부분: 로컬이 아니라 builder에서 복사

# 애플리케이션 포트 명시
EXPOSE 8080

# 컨테이너 시작 시 실행
ENTRYPOINT ["java", "-jar", "app.jar"]