package com.g12.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 状态变更审计日志表。每一次 status 变化都必须落一条。
 * 新建预约时 from_status = -1。
 */
@Data
@TableName("biz_status_change_log")
public class BizStatusChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;

    private Integer fromStatus;

    private Integer toStatus;

    private String action;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime createTime;
}
