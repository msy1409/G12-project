package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 到访登记列表项。
 */
@Data
public class RecordVO {

    private Long id;
    private Long applyId;
    private String applyNo;
    private String visitorName;
    private String deptName;
    private String gate;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private Long stayMinutes;
}
