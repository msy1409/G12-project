package com.g12.visitor.common;

/**
 * 固定业务错误码（与 docs/api.md 第 9 节一致）。
 * Result.message 放这里的常量值，Result.code / HTTP 状态码另算。
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    /** 400 状态机不允许的流转 */
    public static final String STATE_NOT_ALLOWED = "STATE_NOT_ALLOWED";
    /** 400 参数校验失败 */
    public static final String INVALID_PARAM = "INVALID_PARAM";
    /** 401 用户名或密码错误 */
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    /** 401 用户被停用 */
    public static final String USER_DISABLED = "USER_DISABLED";
    /** 401 令牌过期或非法 */
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    /** 403 角色无权访问 */
    public static final String FORBIDDEN = "FORBIDDEN";
    /** 403 审核员越部门操作 */
    public static final String DEPT_SCOPE_DENIED = "DEPT_SCOPE_DENIED";
    /** 404 资源不存在 */
    public static final String NOT_FOUND = "NOT_FOUND";
    /** 409 用户名已存在 */
    public static final String USERNAME_EXISTS = "USERNAME_EXISTS";
    /** 409 部门编码或名称重复 */
    public static final String DEPT_NAME_EXISTS = "DEPT_NAME_EXISTS";
    /** 400 部门下仍有启用用户或未完结预约 */
    public static final String DEPT_IN_USE = "DEPT_IN_USE";
    /** 409 重复入场登记 */
    public static final String DUPLICATE_CHECKIN = "DUPLICATE_CHECKIN";
    /** 400 尚未入场，不能离场 */
    public static final String NOT_CHECKED_IN = "NOT_CHECKED_IN";
    /** 400 核验码错误 */
    public static final String VERIFY_CODE_INVALID = "VERIFY_CODE_INVALID";
    /** 500 未预期异常 */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
