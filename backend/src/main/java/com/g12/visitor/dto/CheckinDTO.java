package com.g12.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 核验入场请求：凭 6 位核验码。
 */
@Data
public class CheckinDTO {

    @NotBlank(message = "核验码不能为空")
    private String verifyCode;

    private String gate;
}
