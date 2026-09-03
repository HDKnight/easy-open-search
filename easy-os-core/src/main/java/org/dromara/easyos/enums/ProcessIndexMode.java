package org.dromara.easyos.enums;

/**
 * 索引处理模式。
 * <p>平滑迁移模式（smoothly）预留给后续版本。
 */
public enum ProcessIndexMode {
    /**
     * 手动模式：启动不自动建索引，需调用 {@code mapper.createIndex()}。
     */
    MANUAL,
    /**
     * 非平滑模式：启动时若索引不存在则自动创建（允许短中断）。
     */
    NOT_SMOOTHLY
}
