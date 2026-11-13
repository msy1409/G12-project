package com.g12.visitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /** v2 新增：当前接口版本标识，/auth/me 与 /auth/login 返回中可见 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String apiVersion;
}
