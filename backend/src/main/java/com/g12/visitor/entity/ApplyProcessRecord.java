package com.g12.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务操作处理记录表：审核、核验入场、离场、取消等动作都要落一条。
 */
@Data
@TableName("apply_process_record")
public class ApplyProcessRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;

    /** CREATE / APPROVE / REJECT / VERIFY_CODE / CHECKIN / CHECKOUT / CANCEL */
    private String action;

    private Long operatorId;

    private String operatorName;

    private String operatorRole;

    private String remark;

    private LocalDateTime createTime;
}
