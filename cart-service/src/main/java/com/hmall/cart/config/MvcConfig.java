package com.hmall.cart.config;

import com.hmall.common.interceptors.UserInfoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加用户信息拦截器，从 header 中获取用户信息并设置到 UserContext
        registry.addInterceptor(new UserInfoInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/favicon.ico",
                        "/v2/**",
                        "/v3/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/doc.html"
                );
    }
}

