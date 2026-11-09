package com.g12.visitor.vo;

import lombok.Data;

/**
 * 统计总览卡片。
 */
@Data
public class StatsOverviewVO {

    private long todayApplyCount;
    private long pendingCount;
    private long todayCheckinCount;
    private long monthCheckinCount;
    private double avgStayMinutes;
}
