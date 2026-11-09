package com.g12.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新建用户请求（密码明文传入，服务端 BCrypt）。
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String phone;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    @NotBlank(message = "角色不能为空")
    private String role;
}
