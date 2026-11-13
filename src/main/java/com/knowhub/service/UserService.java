package com.knowhub.service;

import com.knowhub.dto.user.CurrentUserResponse;
import com.knowhub.dto.user.UserOrganizationTagsResponse;
import com.knowhub.dto.user.UserPageResponse;

public interface UserService {

    /**
     * 注册接口
     * @param username 用户名
     * @param password 密码
     * @param confirmPassword 再次输入密码
     * @return 是否注册成功
     */
    long register(String username, String password, String confirmPassword);

    /**
     * 登录接口
     * @param username 用户名
     * @param password 密码
     * @return 返回 token
     */
    String login(String username, String password);

    /**
     * 返回当前用户信息
     * @param username 用户名
     * @return 当前用户信息封装类（脱敏）
     */
    CurrentUserResponse getCurrentUser(String username);

    /**
     * 分页获取用户列表
     * @param keyword 关键词
     * @param orgTag 组织标签
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 用户分页响应类
     */
    UserPageResponse getUserList(String keyword, String orgTag, Integer status, int page, int size);


    /**
     * 设置当前用户的主组织标签
     *
     * @param username 当前登录用户名
     * @param primaryOrg 要设置为主组织的标签 ID
     */
    void setPrimaryOrg(String username, String primaryOrg);

    /**
     * 获取当前用户的组织标签详情
     *
     * @param username 当前登录用户名
     * @return 组织标签详情
     */
    UserOrganizationTagsResponse getUserOrganizationTags(String username);
}


