package io.github.ninetwo.mdragon.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 幻听：喝下「奶龙的奶」之后，每隔几秒在耳边响一声随机音效（CLAUDE.md 11.3 第 3 条）。
 *
 * <p>音效列表和间隔都是<b>占位值</b>，原设计只写了「定时播放随机音效」，想调直接改下面两个常量。
 *
 * <h2>两个容易踩的点</h2>
 *
 * <p><b>1. {@code shouldApplyEffectTickThisTick} 的第一个参数是「剩余时长」，不是「已经过了多久」。</b>
 * {@code MobEffectInstance.tickServer} 里是这么传的：
 * <pre>{@code
 * int tickCount = this.isInfiniteDuration() ? target.tickCount : this.duration;
 * if (this.effect.value().shouldApplyEffectTickThisTick(tickCount, this.amplifier)
 *         && !this.effect.value().applyEffectTick(serverLevel, target, this.amplifier)) {
 *     return false;   // ← applyEffectTick 返回 false 会把效果直接摘掉
 * }
 * }</pre>
 * 所以 {@code duration % 100 == 0} 是「从末尾往前数每 100 tick 响一次」，
 * 效果是等间隔的。另外 {@link #applyEffectTick} <b>必须返回 true</b>，
 * 返回 false 等于自己把效果删了。
 *
 * <p><b>2. 只放给喝下去的那个人听，所以走封包而不是 {@code level.playSound}。</b>
 * {@code Level.playSound(Entity except, ...)} 的第一个参数是「除了谁」——
 * 把喝的人传进去，结果恰好是全场只有他听不见。这里照抄原版
 * {@code Raid} / {@code PlaySoundCommand} 的写法直接发 {@link ClientboundSoundPacket}。
 */
public class HallucinationEffect extends MobEffect {

    /** 每隔多少 tick 响一次。100 tick = 5 秒。 */
    private static final int INTERVAL_TICKS = 100;

    /** 音量与音调。 */
    private static final float VOLUME = 1.0F;
    private static final float PITCH = 1.0F;

    /**
     * 幻听时会响的音效池，每次随机抽一个。
     *
     * <p>{@code AMBIENT_CAVE} 在 {@code SoundEvents} 里的声明类型是
     * {@code Holder.Reference<SoundEvent>}，其余几个是 {@code SoundEvent}，
     * 所以统一取 {@code .value()} 收成 {@code SoundEvent[]}，
     * 发封包时再用 {@code wrapAsHolder} 转回去。
     */
    private static final SoundEvent[] SOUNDS = {
        SoundEvents.AMBIENT_CAVE.value(),
        SoundEvents.CREEPER_PRIMED,
        SoundEvents.ENDERMAN_STARE,
        SoundEvents.SKELETON_AMBIENT,
        SoundEvents.ZOMBIE_AMBIENT,
        SoundEvents.WITHER_AMBIENT,
        SoundEvents.WARDEN_HEARTBEAT,
        SoundEvents.BELL_RESONATE,
    };

    public HallucinationEffect() {
        // NEUTRAL：只影响 HUD 里的排序和提示文字颜色，没有任何功能后果
        // （全代码库只有 Hud 和 PotionContents 读 MobEffectCategory）。
        // 幻听是这瓶奶的特色而不是惩罚，所以不标成红色的 HARMFUL。
        super(MobEffectCategory.NEUTRAL, 0x6A0DAD);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        // tickCount 是剩余时长（见类注释），别当成已过时长
        return tickCount % INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player) {
            SoundEvent sound = SOUNDS[player.getRandom().nextInt(SOUNDS.length)];
            player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                    SoundSource.AMBIENT,
                    player.getX(), player.getY(), player.getZ(),
                    VOLUME, PITCH,
                    player.getRandom().nextLong()));
        }
        // 返回 true = 效果继续存在；返回 false 会把它摘掉
        return true;
    }
}
