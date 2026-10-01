package io.github.ninetwo.mdragon.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * 奶龙的渲染状态（26.3 的新渲染架构）。
 *
 * <p>实体数据在 {@code extractRenderState} 里从服务端同步来的实体对象拷贝到这个状态对象，
 * 渲染线程只读这个状态，不直接碰实体。
 *
 * <p>这三个字段对应实体上的三个同步状态，阶段 2 会把它们真正驱动起来；
 * 目前渲染器已经按 {@code angry} 切换贴图，只是贴图还没做。
 */
public class MilkDragonRenderState extends LivingEntityRenderState {
    /** 是否处于愤怒状态（换贴图 + 换音效）。 */
    public boolean angry;
    /** 是否在睡觉（静止不动）。 */
    public boolean sleeping;
    /** 是否在大笑（持续约 2 秒）。 */
    public boolean laughing;
}
