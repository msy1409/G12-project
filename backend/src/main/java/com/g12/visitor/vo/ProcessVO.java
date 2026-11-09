package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处理记录条目。
 */
@Data
public class ProcessVO {

    private String action;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String remark;
    private LocalDateTime createTime;
}
