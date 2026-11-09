package com.g12.visitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 离场登记响应。
 */
@Data
public class CheckoutVO {

    private Long applyId;
    private LocalDateTime checkoutTime;
    private Long stayMinutes;
    private Integer status;
}
