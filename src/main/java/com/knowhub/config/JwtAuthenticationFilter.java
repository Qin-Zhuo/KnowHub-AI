package com.knowhub.config;

import com.knowhub.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * JWT 请求过滤器
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils; // 解析并验证JWT
    private final UserDetailsService userDetailsService; // 根据用户名从数据库加载但概念用户、角色和启用状态

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService){
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 从标准 Authorization 请求头读取 Bearer Token。
        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        // 获取 “Bearer ” 开头的token
        String token = authorization.substring(7);

        try{
            // 验证签名和有效期；校验通过后才能读取可信的 Claims。
            /// 这里claims就是转换之后的token信息，进行核对然后转换成claims，看是否相同
            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();

            // 从数据库加载当前用户和当前角色，避免直接依赖令牌里的旧权限。
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if(!userDetails.isEnabled()){
                // 账户已停用时不建立认证身份，受保护接口随后会返回 401。
                SecurityContextHolder.clearContext();
            } else {
                // principal 保存当前用户；authorities 用于后续角色权限判断。
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            // Token 无效或对应用户已不存在时，按未登录请求继续交给安全规则处理。
            SecurityContextHolder.clearContext();
        }

        // 必须继续过滤链，后续安全规则和控制器才能处理请求。
        filterChain.doFilter(request, response);
    }
}
