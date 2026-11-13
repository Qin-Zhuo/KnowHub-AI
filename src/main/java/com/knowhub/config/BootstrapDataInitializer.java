package com.knowhub.config;

import com.knowhub.model.OrganizationTag;
import com.knowhub.model.User;
import com.knowhub.repository.OrganizationTagRepository;
import com.knowhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BootstrapDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final OrganizationTagRepository organizationTagRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.username:}")
    private String username;

    @Value("${app.bootstrap.admin.password:}")
    private String password;

    public BootstrapDataInitializer(
            UserRepository userRepository,
            OrganizationTagRepository organizationTagRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.organizationTagRepository = organizationTagRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "启用管理员初始化时，必须配置管理员用户名和密码"
            );
        }

        User admin = userRepository.findByUsername(username)
                .orElseGet(this::createAdmin);

        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException(
                    "配置的管理员用户名已存在，但该用户不是 ADMIN"
            );
        }

        createTagIfAbsent(
                "default",
                "默认组织",
                "系统默认组织标签，自动分配给新用户",
                admin
        );

        createTagIfAbsent(
                "admin",
                "管理员组织",
                "管理员专用组织标签",
                admin
        );
    }

    private User createAdmin() {
        Instant now = Instant.now();

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setOrgTags("default,admin");
        admin.setPrimaryOrgTag("admin");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);

        return userRepository.save(admin);
    }

    private void createTagIfAbsent(
            String tagId,
            String name,
            String description,
            User creator
    ) {
        if (organizationTagRepository.existsById(tagId)) {
            return;
        }

        Instant now = Instant.now();

        OrganizationTag tag = new OrganizationTag();
        tag.setTagId(tagId);
        tag.setName(name);
        tag.setDescription(description);
        tag.setCreatedBy(creator);
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);

        organizationTagRepository.save(tag);
    }
}