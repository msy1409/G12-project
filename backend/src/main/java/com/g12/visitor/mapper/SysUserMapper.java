package com.g12.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g12.visitor.entity.SysUser;
import com.g12.visitor.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT u.id, u.username, u.real_name AS realName, u.phone,
                   u.dept_id AS deptId, d.dept_name AS deptName,
                   u.role, u.status, u.create_time AS createTime
            FROM sys_user u
            LEFT JOIN sys_department d ON u.dept_id = d.id
            WHERE (#{keyword} IS NULL OR #{keyword} = ''
                   OR u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{deptId} IS NULL OR u.dept_id = #{deptId})
              AND (#{role} IS NULL OR #{role} = '' OR u.role = #{role})
              AND (#{status} IS NULL OR u.status = #{status})
            ORDER BY u.id ASC
            LIMIT #{offset}, #{size}
            """)
    List<UserVO> selectUserPage(@Param("keyword") String keyword,
                                @Param("deptId") Long deptId,
                                @Param("role") String role,
                                @Param("status") Integer status,
                                @Param("offset") int offset,
                                @Param("size") int size);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user u
            WHERE (#{keyword} IS NULL OR #{keyword} = ''
                   OR u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{deptId} IS NULL OR u.dept_id = #{deptId})
              AND (#{role} IS NULL OR #{role} = '' OR u.role = #{role})
              AND (#{status} IS NULL OR u.status = #{status})
            """)
    long countUser(@Param("keyword") String keyword,
                   @Param("deptId") Long deptId,
                   @Param("role") String role,
                   @Param("status") Integer status);

    @Select("""
            SELECT u.id, u.username, u.real_name AS realName, u.phone,
                   u.dept_id AS deptId, d.dept_name AS deptName,
                   u.role, u.status, u.create_time AS createTime
            FROM sys_user u
            LEFT JOIN sys_department d ON u.dept_id = d.id
            WHERE u.id = #{id}
            """)
    UserVO selectUserVOById(@Param("id") Long id);

    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);
}
