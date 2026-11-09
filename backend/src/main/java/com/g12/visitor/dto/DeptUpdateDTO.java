package com.g12.visitor.dto;

import lombok.Data;

/**
 * 修改部门信息。
 */
@Data
public class DeptUpdateDTO {

    private String deptName;

    private String managerName;

    private String phone;
}
