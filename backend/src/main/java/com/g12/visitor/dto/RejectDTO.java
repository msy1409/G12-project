package com.g12.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 驳回请求：reason 必填。
 */
@Data
public class RejectDTO {

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 255, message = "驳回原因最长 255 字符")
    private String reason;
}
