package com.g12.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g12.visitor.entity.ApplyProcessRecord;
import com.g12.visitor.vo.ProcessVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApplyProcessRecordMapper extends BaseMapper<ApplyProcessRecord> {

    @Select("""
            SELECT action, operator_id AS operatorId, operator_name AS operatorName,
                   operator_role AS operatorRole, remark, create_time AS createTime
            FROM apply_process_record
            WHERE apply_id = #{applyId}
            ORDER BY id ASC
            """)
    List<ProcessVO> selectByApplyId(@Param("applyId") Long applyId);
}
