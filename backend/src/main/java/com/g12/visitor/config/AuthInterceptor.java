package com.g12.visitor.config;

import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.CurrentUser;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.common.JwtUtil;
import com.g12.visitor.common.RequireRole;
import com.g12.visitor.entity.SysUser;
import com.g12.visitor.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 认证 + 角色鉴权拦截器：
 * 1. 解析 Authorization: Bearer <token>，校验用户仍存在且启用，写入 AuthContext；
 * 2. 若 Controller 方法/类上有 @RequireRole，校验当前角色是否在允许列表。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            throw BizException.unauthorized(ErrorCode.TOKEN_EXPIRED);
        }
        String token = header.substring(BEARER.length()).trim();
        Claims claims = jwtUtil.parse(token);
        Long userId = jwtUtil.getUserId(claims);

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized(ErrorCode.TOKEN_EXPIRED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.unauthorized(ErrorCode.USER_DISABLED);
        }

        CurrentUser current = new CurrentUser(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole(), user.getDeptId());
        AuthContext.set(current);

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            boolean allowed = Arrays.asList(requireRole.value()).contains(current.getRole());
            if (!allowed) {
                throw BizException.forbidden(ErrorCode.FORBIDDEN);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }
}
