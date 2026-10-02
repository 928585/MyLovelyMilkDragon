package io.github.ninetwo.mdragon;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 本模组自定义的物品数据组件。
 *
 * <p>「奶龙头」戴上之后要做两件跟时间有关的事：30 秒后把佩戴者同化成奶龙、每 5 秒笑一声。
 * 这两件事的进度都<b>存在物品自己身上</b>，跟着物品走——谁戴都行，不需要额外的全局表。
 *
 * <p>存的是<b>游戏刻（game time）</b>而不是「还剩多少秒」，所以只在戴上时写一次，
 * 之后每 tick 只读不写。这一点很重要：每 tick 写组件会让物品每 tick 往客户端同步一次，
 * 20 包/秒纯属浪费。
 */
public class ModDataComponents {

    /**
     * 戴上奶龙头之后，到第几个游戏刻把佩戴者同化成奶龙。
     *
     * <p>没有这个组件 = 还没记过时间，由 {@code inventoryTick} 补上。
     */
    public static final DataComponentType<Long> CONVERSION_DEADLINE =
            register("conversion_deadline", Codec.LONG);

    /** 下一次该笑一声的游戏刻。同样只在笑完之后才改写。 */
    public static final DataComponentType<Long> NEXT_LAUGH_TICK =
            register("next_laugh_tick", Codec.LONG);

    /**
     * 注册一个持久化组件。
     *
     * <p>{@code persistent(Codec)} 是必须的：不写它组件就进不了存档，
     * 世界一保存再读回来，倒计时就没了。
     */
    private static <T> DataComponentType<T> register(String name, Codec<T> codec) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Mylovelymilkdragon.id(name),
                DataComponentType.<T>builder().persistent(codec).build());
    }

    public static void registerModDataComponents() {
        // 空方法，触发类加载
    }
}
