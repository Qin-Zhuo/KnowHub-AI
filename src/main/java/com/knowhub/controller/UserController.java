package com.knowhub.controller;

import com.knowhub.common.ApiResponse;
import com.knowhub.common.ResultUtils;
import com.knowhub.dto.user.*;
import com.knowhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    /**
     * 用户注册接口
     * @param registerRequest 注册请求封装类
     * @return 注册成功的用户Id
     */
    @PostMapping("/register")
    public ApiResponse<Long> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.username();
        String password = registerRequest.password();
        String confirmPassword = registerRequest.confirmPassword();

        Long id = userService.register(username, password, confirmPassword);

        return ResultUtils.success(id);
    }

    /**
     * 获取登录token请求
     * @param loginRequest 登录请求
     * @return 返回一个jwt token的封装类
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        String username = loginRequest.username();
        String password = loginRequest.password();

        String token = userService.login(username, password);

        return ResultUtils.success(new LoginResponse(token));
    }

    /**
     * 携带token去请求登录的接口
     * @param userDetails jwt请求过滤器会拦截请求然后解析token获取到payload（claims）去校验是否合法，合法后将userDetails传递给controller
     * @return 脱敏的用户信息
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        CurrentUserResponse response = userService.getCurrentUser(userDetails.getUsername());

        return ResultUtils.success(response);
    }

    @PutMapping("/primary-org")
    public ApiResponse<Void> setPrimaryOrgTagId(
            @Valid @RequestBody SetPrimaryOrgRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        userService.setPrimaryOrgTagId(userDetails.getUsername(), request.primaryOrgTagId());

        return ResultUtils.success(null);
    }

    @GetMapping("/org-tags")
    public ApiResponse<UserOrganizationTagsResponse> getUserOrganizationTags(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserOrganizationTagsResponse response = userService.getUserOrganizationTags(userDetails.getUsername());

        return ResultUtils.success(response);
    }
}
