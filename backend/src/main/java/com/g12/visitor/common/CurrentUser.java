package com.g12.visitor.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户，由 JwtInterceptor 解析 token 后放入 AuthContext。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private Long deptId;

    public boolean isAdmin() {
        return RoleConstant.ADMIN.equals(role);
    }

    public boolean isAuditor() {
        return RoleConstant.AUDITOR.equals(role);
    }

    public boolean isEmployee() {
        return RoleConstant.EMPLOYEE.equals(role);
    }
}
