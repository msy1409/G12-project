package com.g12.visitor.vo;

import lombok.Data;

/**
 * 部门视图（下拉框、管理页共用）。
 */
@Data
public class DeptVO {

    private Long id;
    private String deptCode;
    private String deptName;
    private String managerName;
    private String phone;
    private Integer status;
}
