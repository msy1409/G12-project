package com.g12.visitor.common;

/**
 * 预约单主状态（与 docs/requirements.md 状态机一致）。
 */
public final class ApplyStatus {

    private ApplyStatus() {
    }

    /** 待审核 */
    public static final int PENDING = 0;
    /** 审核通过 */
    public static final int APPROVED = 1;
    /** 待到访 */
    public static final int WAITING_VISIT = 2;
    /** 已到访 */
    public static final int CHECKED_IN = 3;
    /** 已离开 */
    public static final int CHECKED_OUT = 4;
    /** 驳回 / 取消 */
    public static final int CLOSED = 9;

    /** 新建预约时审计表的 from_status 记为 -1（表示"从无到有"） */
    public static final int FROM_NONE = -1;
}
