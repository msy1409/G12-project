package com.g12.visitor.dto;

import lombok.Data;

/**
 * 通用备注/原因请求体（approve 的 remark、reject/cancel 的 reason 都兼容）。
 */
@Data
public class RemarkDTO {

    private String remark;

    private String reason;

    public String pick() {
        if (remark != null && !remark.isBlank()) {
            return remark;
        }
        return reason;
    }
}
