package com.knowhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowhub.common.ResultUtils;
import com.knowhub.exception.ErrorCode;
import com.knowhub.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtUtils jwtUtils, UserDetailsService userDetailsService,
            ObjectMapper objectMapper) throws Exception {

        // 创建过滤器时传入jwtUtils和userDetailsService
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/users/register",
                                "/api/v1/users/login",
                                "/api/v1/health",
                                "/api/v1/ok"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ResultUtils.error(ErrorCode.UNAUTHORIZED)
                            );
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    response.getOutputStream(),
                                    ResultUtils.error(ErrorCode.FORBIDDEN)
                            );
                        })
                )
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}