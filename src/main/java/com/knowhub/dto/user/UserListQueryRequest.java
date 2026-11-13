package com.knowhub.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserListQueryRequest {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数至少为1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer size = 20;

    @Size(max = 255, message = "关键词长度不能超过255")
    private String keyword;

    @Size(max = 300, message = "组织标签长度不能超过300")
    private String orgTag;

    @Min(value = 0, message = "status只能是0或1")
    @Max(value = 1, message = "status只能是0或1")
    private Integer status;
}