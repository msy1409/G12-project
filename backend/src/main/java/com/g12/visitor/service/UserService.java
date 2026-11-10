package com.g12.visitor.service;

import com.g12.visitor.common.AuthContext;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.common.PageQuery;
import com.g12.visitor.common.PageResult;
import com.g12.visitor.common.RoleConstant;
import com.g12.visitor.dto.UserCreateDTO;
import com.g12.visitor.dto.UserUpdateDTO;
import com.g12.visitor.entity.SysUser;
import com.g12.visitor.mapper.SysUserMapper;
import com.g12.visitor.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final Set<String> ROLES = Set.of(RoleConstant.EMPLOYEE, RoleConstant.AUDITOR, RoleConstant.ADMIN);

    private final SysUserMapper userMapper;

    public PageResult<UserVO> page(Integer page, Integer size, String keyword, String role, Long deptId, Integer status) {
        int p = PageQuery.page(page);
        int s = PageQuery.size(size);
        List<UserVO> list = userMapper.selectUserPage(keyword, deptId, role, status, PageQuery.offset(p, s), s);
        long total = userMapper.countUser(keyword, deptId, role, status);
        return PageResult.of(list, total, p, s);
    }

    public UserVO detail(Long id) {
        UserVO vo = userMapper.selectUserVOById(id);
        if (vo == null) {
            throw BizException.notFound();
        }
        return vo;
    }

    public Long create(UserCreateDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw BizException.conflict(ErrorCode.USERNAME_EXISTS);
        }
        if (!ROLES.contains(dto.getRole())) {
            throw BizException.badRequest(ErrorCode.INVALID_PARAM);
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(AuthService.encode(dto.getPassword() == null || dto.getPassword().isBlank()
                ? DEFAULT_PASSWORD : dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setRole(dto.getRole());
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    public void update(Long id, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound();
        }
        if (dto.getRole() != null && !ROLES.contains(dto.getRole())) {
            throw BizException.badRequest(ErrorCode.INVALID_PARAM);
        }
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setRealName(dto.getRealName());
        patch.setPhone(dto.getPhone());
        patch.setDeptId(dto.getDeptId());
        patch.setRole(dto.getRole());
        userMapper.updateById(patch);
    }

    /**
     * 逻辑删除：status=0，保留历史外键与审计。
     */
    public void disable(Long id) {
        if (AuthContext.get().getId().equals(id)) {
            throw BizException.badRequest(ErrorCode.INVALID_PARAM);
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound();
        }
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setStatus(0);
        userMapper.updateById(patch);
    }

    public void resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound();
        }
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setPassword(AuthService.encode(DEFAULT_PASSWORD));
        userMapper.updateById(patch);
    }
}
