package com.g12.visitor.dto;

import lombok.Data;

/**
 * 修改用户基本信息（不含密码）。
 */
@Data
public class UserUpdateDTO {

    private String realName;

    private String phone;

    private Long deptId;

    private String role;
}
