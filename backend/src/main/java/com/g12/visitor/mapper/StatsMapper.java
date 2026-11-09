package com.g12.visitor.mapper;

import com.g12.visitor.vo.DateCountVO;
import com.g12.visitor.vo.DeptStatsVO;
import com.g12.visitor.vo.StatsOverviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 统计接口专用查询。deptId 为 null 表示全局（ADMIN），否则限定部门（AUDITOR）。
 */
@Mapper
public interface StatsMapper {

    @Select("""
            SELECT
              (SELECT COUNT(1) FROM visitor_apply
                WHERE DATE(create_time) = CURDATE()
                  AND (#{deptId} IS NULL OR dept_id = #{deptId})) AS todayApplyCount,
              (SELECT COUNT(1) FROM visitor_apply
                WHERE status = 0
                  AND (#{deptId} IS NULL OR dept_id = #{deptId})) AS pendingCount,
              (SELECT COUNT(1) FROM visitor_record r
                LEFT JOIN visitor_apply a ON r.apply_id = a.id
                WHERE DATE(r.checkin_time) = CURDATE()
                  AND (#{deptId} IS NULL OR a.dept_id = #{deptId})) AS todayCheckinCount,
              (SELECT COUNT(1) FROM visitor_record r
                LEFT JOIN visitor_apply a ON r.apply_id = a.id
                WHERE YEAR(r.checkin_time) = YEAR(CURDATE())
                  AND MONTH(r.checkin_time) = MONTH(CURDATE())
                  AND (#{deptId} IS NULL OR a.dept_id = #{deptId})) AS monthCheckinCount,
              (SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, r.checkin_time, r.checkout_time)), 0)
                FROM visitor_record r
                LEFT JOIN visitor_apply a ON r.apply_id = a.id
                WHERE r.checkout_time IS NOT NULL
                  AND (#{deptId} IS NULL OR a.dept_id = #{deptId})) AS avgStayMinutes
            """)
    StatsOverviewVO selectOverview(@Param("deptId") Long deptId);

    @Select("""
            SELECT DATE(create_time) AS date, COUNT(1) AS cnt
            FROM visitor_apply
            WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
              AND (#{deptId} IS NULL OR dept_id = #{deptId})
            GROUP BY DATE(create_time)
            """)
    List<DateCountVO> selectApplyTrend(@Param("deptId") Long deptId, @Param("days") int days);

    @Select("""
            SELECT DATE(r.checkin_time) AS date, COUNT(1) AS cnt
            FROM visitor_record r
            LEFT JOIN visitor_apply a ON r.apply_id = a.id
            WHERE r.checkin_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
              AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
            GROUP BY DATE(r.checkin_time)
            """)
    List<DateCountVO> selectCheckinTrend(@Param("deptId") Long deptId, @Param("days") int days);

    @Select("""
            SELECT d.id AS deptId, d.dept_name AS deptName,
                   COUNT(a.id) AS applyCount,
                   SUM(CASE WHEN a.status IN (3,4) THEN 1 ELSE 0 END) AS checkinCount
            FROM sys_department d
            LEFT JOIN visitor_apply a ON a.dept_id = d.id
            WHERE d.status = 1
            GROUP BY d.id, d.dept_name
            ORDER BY d.id ASC
            """)
    List<DeptStatsVO> selectDeptStats();
}
