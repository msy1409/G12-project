package com.g12.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g12.visitor.entity.VisitorApply;
import com.g12.visitor.vo.ApplyDetailVO;
import com.g12.visitor.vo.ApplyListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VisitorApplyMapper extends BaseMapper<VisitorApply> {

    String BASE_COLS = """
            a.id, a.apply_no AS applyNo, a.visitor_name AS visitorName, a.visitor_phone AS visitorPhone,
            a.visitor_company AS visitorCompany, a.dept_id AS deptId, d.dept_name AS deptName,
            a.host_name AS hostName, a.visit_reason AS visitReason,
            a.visit_time AS visitTime, a.leave_time AS leaveTime,
            a.verify_code AS verifyCode, a.status, a.create_time AS createTime
            """;

    String FROM_JOIN = " FROM visitor_apply a LEFT JOIN sys_department d ON a.dept_id = d.id ";

    @Select("SELECT " + BASE_COLS + FROM_JOIN + """
            WHERE a.create_by = #{createBy}
              AND (#{status} IS NULL OR a.status = #{status})
            ORDER BY a.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<ApplyListItemVO> selectMine(@Param("createBy") Long createBy,
                                     @Param("status") Integer status,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    @Select("""
            SELECT COUNT(1) FROM visitor_apply a
            WHERE a.create_by = #{createBy}
              AND (#{status} IS NULL OR a.status = #{status})
            """)
    long countMine(@Param("createBy") Long createBy, @Param("status") Integer status);

    @Select("SELECT " + BASE_COLS + FROM_JOIN + """
            WHERE a.status = 0
              AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR a.visitor_name LIKE CONCAT('%', #{keyword}, '%')
                   OR a.apply_no LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY a.id ASC
            LIMIT #{offset}, #{size}
            """)
    List<ApplyListItemVO> selectPending(@Param("deptId") Long deptId,
                                        @Param("keyword") String keyword,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    @Select("""
            SELECT COUNT(1) FROM visitor_apply a
            WHERE a.status = 0
              AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR a.visitor_name LIKE CONCAT('%', #{keyword}, '%')
                   OR a.apply_no LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countPending(@Param("deptId") Long deptId, @Param("keyword") String keyword);

    @Select("SELECT " + BASE_COLS + FROM_JOIN + """
            WHERE (#{deptId} IS NULL OR a.dept_id = #{deptId})
              AND (#{status} IS NULL OR a.status = #{status})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR a.visitor_name LIKE CONCAT('%', #{keyword}, '%')
                   OR a.visitor_phone LIKE CONCAT('%', #{keyword}, '%')
                   OR a.apply_no LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{startDate} IS NULL OR #{startDate} = '' OR a.visit_time >= #{startDate})
              AND (#{endDate} IS NULL OR #{endDate} = '' OR a.visit_time <= CONCAT(#{endDate}, ' 23:59:59'))
            ORDER BY a.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<ApplyListItemVO> selectAuditList(@Param("deptId") Long deptId,
                                          @Param("status") Integer status,
                                          @Param("keyword") String keyword,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    @Select("""
            SELECT COUNT(1) FROM visitor_apply a
            WHERE (#{deptId} IS NULL OR a.dept_id = #{deptId})
              AND (#{status} IS NULL OR a.status = #{status})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR a.visitor_name LIKE CONCAT('%', #{keyword}, '%')
                   OR a.visitor_phone LIKE CONCAT('%', #{keyword}, '%')
                   OR a.apply_no LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{startDate} IS NULL OR #{startDate} = '' OR a.visit_time >= #{startDate})
              AND (#{endDate} IS NULL OR #{endDate} = '' OR a.visit_time <= CONCAT(#{endDate}, ' 23:59:59'))
            """)
    long countAuditList(@Param("deptId") Long deptId,
                        @Param("status") Integer status,
                        @Param("keyword") String keyword,
                        @Param("startDate") String startDate,
                        @Param("endDate") String endDate);

    @Select("SELECT " + BASE_COLS + FROM_JOIN + " WHERE a.id = #{id} ")
    ApplyDetailVO selectDetail(@Param("id") Long id);

    @Select("SELECT * FROM visitor_apply WHERE verify_code = #{verifyCode} LIMIT 1")
    VisitorApply selectByVerifyCode(@Param("verifyCode") String verifyCode);

    @Select("""
            SELECT COUNT(1) FROM visitor_apply
            WHERE dept_id = #{deptId} AND status IN (0,1,2,3)
            """)
    long countUnfinishedByDept(@Param("deptId") Long deptId);

    /**
     * 乐观锁式状态更新：只有当前状态仍等于 fromStatus 才更新。
     * 返回 0 表示状态已被别人改过（并发冲突）。
     */
    @Update("""
            UPDATE visitor_apply
            SET status = #{toStatus}, update_time = NOW()
            WHERE id = #{id} AND status = #{fromStatus}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("fromStatus") int fromStatus,
                     @Param("toStatus") int toStatus);

    @Update("UPDATE visitor_apply SET verify_code = #{verifyCode}, update_time = NOW() WHERE id = #{id}")
    int updateVerifyCode(@Param("id") Long id, @Param("verifyCode") String verifyCode);
}
