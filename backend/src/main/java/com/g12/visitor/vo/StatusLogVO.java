package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 状态变更审计条目。
 */
@Data
public class StatusLogVO {

    private Integer fromStatus;
    private Integer toStatus;
    private String action;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
