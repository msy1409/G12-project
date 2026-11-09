package com.g12.visitor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用/停用用户。
 */
@Data
public class StatusDTO {

    @NotNull(message = "status 不能为空")
    private Integer status;
}
