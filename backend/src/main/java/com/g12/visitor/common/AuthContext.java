package com.g12.visitor.common;

/**
 * 请求级登录用户上下文（ThreadLocal）。
 */
public final class AuthContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized(ErrorCode.TOKEN_EXPIRED);
        }
        return user;
    }

    public static CurrentUser getOrNull() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
