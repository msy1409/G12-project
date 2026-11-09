package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约列表项（我的预约 / 待审核 / 审核记录共用基础字段）。
 */
@Data
public class ApplyListItemVO {

    private Long id;
    private String applyNo;
    private String visitorName;
    private String visitorPhone;
    private String visitorCompany;
    private Long deptId;
    private String deptName;
    private String hostName;
    private String visitReason;
    private LocalDateTime visitTime;
    private LocalDateTime leaveTime;
    private String verifyCode;
    private Integer status;
    private LocalDateTime createTime;
}
