package com.g12.visitor.vo;

import lombok.Data;

/**
 * 部门维度统计。
 */
@Data
public class DeptStatsVO {

    private Long deptId;
    private String deptName;
    private long applyCount;
    private long checkinCount;
}
