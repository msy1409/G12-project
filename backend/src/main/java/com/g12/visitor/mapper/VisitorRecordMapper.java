package com.g12.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g12.visitor.entity.VisitorRecord;
import com.g12.visitor.vo.RecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VisitorRecordMapper extends BaseMapper<VisitorRecord> {

    String BASE_COLS = """
            r.id, r.apply_id AS applyId, a.apply_no AS applyNo, a.visitor_name AS visitorName,
            d.dept_name AS deptName, r.gate, r.checkin_time AS checkinTime, r.checkout_time AS checkoutTime,
            TIMESTAMPDIFF(MINUTE, r.checkin_time, r.checkout_time) AS stayMinutes
            """;

    @Select("SELECT " + BASE_COLS + """
            FROM visitor_record r
            LEFT JOIN visitor_apply a ON r.apply_id = a.id
            LEFT JOIN sys_department d ON a.dept_id = d.id
            WHERE (#{date} IS NULL OR #{date} = '' OR DATE(r.checkin_time) = #{date})
              AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR a.visitor_name LIKE CONCAT('%', #{keyword}, '%')
                   OR a.apply_no LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY r.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<RecordVO> selectRecordPage(@Param("date") String date,
                                    @Param("deptId") Long deptId,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    @Select("""
            SELECT COUNT(1) FROM visitor_record r
            LEFT JOIN visitor_apply a ON r.apply_id = a.id
            WHERE (#{date} IS NULL OR #{date} = '' OR DATE(r.checkin_time) = #{date})
              AND (#{deptId} IS NULL OR a.dept_id = #{deptId})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR a.visitor_name LIKE CONCAT('%', #{keyword}, '%')
                   OR a.apply_no LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countRecord(@Param("date") String date,
                     @Param("deptId") Long deptId,
                     @Param("keyword") String keyword);

    @Select("SELECT * FROM visitor_record WHERE apply_id = #{applyId} LIMIT 1")
    VisitorRecord selectByApplyId(@Param("applyId") Long applyId);
}
