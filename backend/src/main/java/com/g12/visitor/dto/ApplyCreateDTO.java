package com.g12.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建预约请求。
 */
@Data
public class ApplyCreateDTO {

    @NotBlank(message = "访客姓名不能为空")
    private String visitorName;

    @NotBlank(message = "访客电话不能为空")
    private String visitorPhone;

    private String visitorCompany;

    /** 被访部门；为空时取当前用户部门 */
    private Long deptId;

    @NotBlank(message = "受访人不能为空")
    private String hostName;

    private String visitReason;

    @NotNull(message = "来访时间不能为空")
    private LocalDateTime visitTime;

    private LocalDateTime leaveTime;
}
