package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约详情：主单 + 处理记录 + 状态变更历史。
 */
@Data
public class ApplyDetailVO {

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

    private List<ProcessVO> processList;
    private List<StatusLogVO> statusLogList;
}
