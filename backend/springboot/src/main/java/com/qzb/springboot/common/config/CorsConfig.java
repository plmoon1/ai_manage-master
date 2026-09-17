package com.qzb.springboot.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.addAllowedOrigin("*"); // 1 设置访问源地址
//        corsConfiguration.addAllowedHeader("*"); // 2 设置访问源请求头
//        corsConfiguration.addAllowedMethod("*"); // 3 设置访问源请求方法
        // 1. 修复：兼容Spring Boot 2.4+，允许所有源（生产环境建议指定具体域名）
        corsConfiguration.addAllowedOriginPattern("*");
        // 2. 关键：允许携带凭证（跨域下载必需）
        corsConfiguration.setAllowCredentials(true);
        // 3. 允许所有请求头
        corsConfiguration.addAllowedHeader("*");
        // 4. 允许所有请求方法
        corsConfiguration.addAllowedMethod("*");
        // 5. 核心：暴露下载关键响应头（让浏览器能读取Content-Disposition）
        corsConfiguration.addExposedHeader("Content-Disposition");
        // 6. 预检请求缓存时间（避免重复预检）
        corsConfiguration.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", corsConfiguration); // 4 对接口配置跨域设置
        return new CorsFilter(source);
    }
}