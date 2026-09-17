package com.qzb.springboot.common.config;

import cn.hutool.log.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.DigestUtils;
import cn.hutool.log.Log;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) // 开启注解权限
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Log log = LogFactory.get();
    // 密码编码器：兼容你原有32位MD5密码
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return DigestUtils.md5DigestAsHex(rawPassword.toString().getBytes());
            }
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals(encode(rawPassword));
            }
        };
    }

    // 安全拦截规则
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
//                .antMatchers("/login", "/register").permitAll()
                // 所有接口需要认证
//                .anyRequest().authenticated()
                // 放行所有请求，让JWT拦截器先执行
                .anyRequest().permitAll()
                .and()
                // 未登录处理
                .exceptionHandling()
                .authenticationEntryPoint((req, resp, e) -> {
                    resp.setContentType("application/json;charset=utf-8");
                    resp.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(ResultCodeEnum.TOKEN_CHECK_ERROR)));
                })
                // 无权限处理
                .accessDeniedHandler((req, resp, e) -> {
                    resp.setContentType("application/json;charset=utf-8");
                    resp.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(ResultCodeEnum.Authority_ERROR)));
                    log.error("权限校验失败");
                });
    }
}
