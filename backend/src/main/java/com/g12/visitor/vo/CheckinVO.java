package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核验入场响应。
 */
@Data
public class CheckinVO {

    private Long recordId;
    private Long applyId;
    private String visitorName;
    private LocalDateTime checkinTime;
    private Integer status;
}
