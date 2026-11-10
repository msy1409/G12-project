package com.g12.visitor.service;

import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.common.JwtUtil;
import com.g12.visitor.dto.LoginDTO;
import com.g12.visitor.entity.SysUser;
import com.g12.visitor.mapper.SysUserMapper;
import com.g12.visitor.vo.LoginVO;
import com.g12.visitor.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectByUsername(dto.getUsername());
        if (user == null || !ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw BizException.unauthorized(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden(ErrorCode.USER_DISABLED);
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole(), user.getDeptId());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(userMapper.selectUserVOById(user.getId()));
        return vo;
    }

    public UserVO currentUser() {
        return userMapper.selectUserVOById(AuthContext.get().getId());
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }
}
