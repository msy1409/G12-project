package com.g12.visitor.vo;

/**
 * 创建预约的响应，字段与 docs/api.md 5.1 对齐：{ id, applyNo, status }
 */
public record ApplyCreateVO(Long id, String applyNo, Integer status) {
}
