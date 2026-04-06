package com.example.dailymenu.shared.adapter.in.web;

import com.example.dailymenu.shared.adapter.in.web.dto.ApiResponse;
import com.example.dailymenu.shared.adapter.in.web.dto.ErrorResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 Controller 응답을 ApiResponse로 자동 래핑한다 (api-spec.md S3).
 * ErrorResponse, String, ApiResponse는 래핑하지 않고 그대로 통과시킨다.
 */
@RestControllerAdvice(basePackages = "com.example.dailymenu")
public class ApiResponseWrappingAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ErrorResponse || body instanceof ApiResponse || body instanceof String) {
            return body;
        }
        return ApiResponse.ok(body);
    }
}
