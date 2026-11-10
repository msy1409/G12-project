package com.g12.visitor.common;

/**
 * 分页参数归一化：page 从 1 开始，size 默认 10、上限 100。
 */
public final class PageQuery {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private PageQuery() {
    }

    public static int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int size(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static int offset(int page, int size) {
        return (page - 1) * size;
    }
}
