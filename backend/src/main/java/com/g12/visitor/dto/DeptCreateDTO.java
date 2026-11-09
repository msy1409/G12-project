package com.g12.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新建部门。
 */
@Data
public class DeptCreateDTO {

    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private String managerName;

    private String phone;
}
