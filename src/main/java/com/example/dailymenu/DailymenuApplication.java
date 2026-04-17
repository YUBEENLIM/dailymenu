package com.example.dailymenu;

import com.example.dailymenu.place.adapter.out.kakao.KakaoPlaceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KakaoPlaceProperties.class, com.example.dailymenu.user.adapter.out.auth.kakao.KakaoOAuthProperties.class, com.example.dailymenu.shared.adapter.out.cache.RateLimitProperties.class})
public class DailymenuApplication {

	public static void main(String[] args) {
		SpringApplication.run(DailymenuApplication.class, args);
	}

}
