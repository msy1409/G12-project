package com.g12.visitor.vo;

import lombok.Data;

/**
 * 近 N 天趋势点。
 */
@Data
public class TrendVO {

    private String date;
    private long applyCount;
    private long checkinCount;
}
