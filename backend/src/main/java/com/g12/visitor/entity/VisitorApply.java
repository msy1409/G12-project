package com.g12.visitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约主单表。status 取值见 common.ApplyStatus。
 */
@Data
@TableName("visitor_apply")
public class VisitorApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applyNo;

    private String visitorName;

    private String visitorPhone;

    private String visitorCompany;

    /** 被访部门 */
    private Long deptId;

    private String hostName;

    private String visitReason;

    private LocalDateTime visitTime;

    private LocalDateTime leaveTime;

    /** 6 位数字核验码，审核通过时生成 */
    private String verifyCode;

    /** 0待审核 1审核通过 2待到访 3已到访 4已离开 9驳回/取消 */
    private Integer status;

    /** 提交人员工 id */
    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
