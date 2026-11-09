package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表/详情视图。永远不含 password。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Long deptId;
    private String deptName;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
}
