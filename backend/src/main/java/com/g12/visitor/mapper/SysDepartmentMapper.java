package com.g12.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g12.visitor.entity.SysDepartment;
import com.g12.visitor.vo.DeptVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    @Select("""
            SELECT id, dept_code AS deptCode, dept_name AS deptName,
                   manager_name AS managerName, phone, status
            FROM sys_department
            WHERE (#{status} IS NULL OR status = #{status})
            ORDER BY id ASC
            """)
    List<DeptVO> selectDeptList(@Param("status") Integer status);
}
