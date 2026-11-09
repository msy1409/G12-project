package com.g12.visitor.vo;

import lombok.Data;

/**
 * 按日期聚合的计数（统计接口内部用）。
 */
@Data
public class DateCountVO {

    private String date;
    private long cnt;
}
