package com.g12.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 到访/离场登记表。一个预约最多一条（uk_record_apply）。
 */
@Data
@TableName("visitor_record")
public class VisitorRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;

    private LocalDateTime checkinTime;

    private LocalDateTime checkoutTime;

    /** 核验入场操作人 */
    private Long checkinBy;

    /** 离场操作人 */
    private Long checkoutBy;

    private String gate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
