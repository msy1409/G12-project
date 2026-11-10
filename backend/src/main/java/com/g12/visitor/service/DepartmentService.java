package com.g12.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.g12.visitor.common.BizException;
import com.g12.visitor.common.ErrorCode;
import com.g12.visitor.dto.DeptCreateDTO;
import com.g12.visitor.dto.DeptUpdateDTO;
import com.g12.visitor.entity.SysDepartment;
import com.g12.visitor.mapper.SysDepartmentMapper;
import com.g12.visitor.mapper.SysUserMapper;
import com.g12.visitor.mapper.VisitorApplyMapper;
import com.g12.visitor.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final SysDepartmentMapper deptMapper;
    private final SysUserMapper userMapper;
    private final VisitorApplyMapper applyMapper;

    public List<DeptVO> list(Integer status) {
        return deptMapper.selectDeptList(status);
    }

    public Long create(DeptCreateDTO dto) {
        if (existsCode(dto.getDeptCode(), null)) {
            throw BizException.conflict(ErrorCode.DEPT_NAME_EXISTS);
        }
        if (existsName(dto.getDeptName(), null)) {
            throw BizException.conflict(ErrorCode.DEPT_NAME_EXISTS);
        }
        SysDepartment dept = new SysDepartment();
        dept.setDeptCode(dto.getDeptCode());
        dept.setDeptName(dto.getDeptName());
        dept.setManagerName(dto.getManagerName());
        dept.setPhone(dto.getPhone());
        dept.setStatus(1);
        deptMapper.insert(dept);
        return dept.getId();
    }

    public void update(Long id, DeptUpdateDTO dto) {
        SysDepartment dept = deptMapper.selectById(id);
        if (dept == null) {
            throw BizException.notFound();
        }
        if (dto.getDeptName() != null && existsName(dto.getDeptName(), id)) {
            throw BizException.conflict(ErrorCode.DEPT_NAME_EXISTS);
        }
        SysDepartment patch = new SysDepartment();
        patch.setId(id);
        patch.setDeptName(dto.getDeptName());
        patch.setManagerName(dto.getManagerName());
        patch.setPhone(dto.getPhone());
        deptMapper.updateById(patch);
    }

    /**
     * 部门下还有启用用户或未完结预约时禁止删除，统一改为逻辑删除 status=0。
     */
    public void disable(Long id) {
        SysDepartment dept = deptMapper.selectById(id);
        if (dept == null) {
            throw BizException.notFound();
        }
        long users = userMapper.selectCount(new LambdaQueryWrapper<com.g12.visitor.entity.SysUser>()
                .eq(com.g12.visitor.entity.SysUser::getDeptId, id)
                .eq(com.g12.visitor.entity.SysUser::getStatus, 1));
        if (users > 0 || applyMapper.countUnfinishedByDept(id) > 0) {
            throw BizException.conflict(ErrorCode.DEPT_IN_USE);
        }
        SysDepartment patch = new SysDepartment();
        patch.setId(id);
        patch.setStatus(0);
        deptMapper.updateById(patch);
    }

    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<SysDepartment> w = new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getDeptCode, code);
        if (excludeId != null) {
            w.ne(SysDepartment::getId, excludeId);
        }
        return deptMapper.selectCount(w) > 0;
    }

    private boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<SysDepartment> w = new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getDeptName, name);
        if (excludeId != null) {
            w.ne(SysDepartment::getId, excludeId);
        }
        return deptMapper.selectCount(w) > 0;
    }
}
