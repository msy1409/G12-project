package com.g12.visitor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 离场登记请求：按预约单 id。
 */
@Data
public class CheckoutDTO {

    @NotNull(message = "applyId 不能为空")
    private Long applyId;

    private String gate;
}
