package com.knowhub.service.impl;

import com.knowhub.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceI implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceI(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.knowhub.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 将业务用户 转换为 Spring Security 识别的身份对象。
        UserDetails userDetails = User.withUsername(user.getUsername()) // 设置安全身份的用户名
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name()) // 角色转换为权限，供 hasRole 等规则判断
                .disabled(!user.isEnabled()) // Builder 接收“是否禁用”，因此与数据库 enabled 取反
                .build();

        return userDetails;
    }
}
