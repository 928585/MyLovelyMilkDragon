package io.github.ninetwo.mdragon;

import io.github.ninetwo.mdragon.effect.HallucinationEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 自定义状态效果。
 *
 * <p>这里两个效果都是「奶龙的奶」喝下去之后附加的（CLAUDE.md 11.3 的第 2、3 条）：
 * <ul>
 *   <li>{@link #MILK_DRAGON_ARMOR} —— +5 护甲，靠属性修饰符生效</li>
 *   <li>{@link #MILK_DRAGON_HALLUCINATION} —— 幻听，逻辑在 {@link HallucinationEffect}</li>
 * </ul>
 *
 * <p>⚠️ 空壳 {@code MobEffect} <b>什么都不会发生</b>：想真正生效必须覆写
 * {@code applyEffectTick} / {@code shouldApplyEffectTickThisTick}，
 * 或者像护甲那样挂一个属性修饰符。
 *
 * <p>翻译键是 {@code effect.<命名空间>.<路径>}，见 lang 文件。
 */
public class ModEffects {

    /** 护甲加值：5 点。改数值改这里。 */
    private static final double ARMOR_BONUS = 5.0D;

    /**
     * +5 护甲。
     *
     * <p>{@code addAttributeModifier} 返回的就是它自己（源码里只有一句 {@code return this}），
     * 所以可以链式调用完再交给注册表。
     *
     * <p>⚠️ 这里的 {@code id} 是<b>修饰符自己的标识</b>，不是效果 id。同一属性上同 id 的修饰符
     * 只会存在一条，所以喝第二瓶是刷新时长而不是叠加护甲——正好是我们想要的。
     */
    public static final Holder<MobEffect> MILK_DRAGON_ARMOR = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Mylovelymilkdragon.id("milk_dragon_armor"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xEAF6FF) {}
                    .addAttributeModifier(
                            Attributes.ARMOR,
                            Mylovelymilkdragon.id("milk_dragon_armor"),
                            ARMOR_BONUS,
                            AttributeModifier.Operation.ADD_VALUE));

    /** 幻听：定时给喝下去的人播放随机音效。 */
    public static final Holder<MobEffect> MILK_DRAGON_HALLUCINATION = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Mylovelymilkdragon.id("milk_dragon_hallucination"),
            new HallucinationEffect());

    public static void registerModEffects() {
        // 空方法，触发类加载
    }
}
