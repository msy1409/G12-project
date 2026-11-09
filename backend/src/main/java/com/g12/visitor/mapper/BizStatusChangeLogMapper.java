package com.g12.visitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g12.visitor.entity.BizStatusChangeLog;
import com.g12.visitor.vo.StatusLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BizStatusChangeLogMapper extends BaseMapper<BizStatusChangeLog> {

    @Select("""
            SELECT from_status AS fromStatus, to_status AS toStatus, action,
                   operator_name AS operatorName, remark, create_time AS createTime
            FROM biz_status_change_log
            WHERE apply_id = #{applyId}
            ORDER BY id ASC
            """)
    List<StatusLogVO> selectByApplyId(@Param("applyId") Long applyId);
}
