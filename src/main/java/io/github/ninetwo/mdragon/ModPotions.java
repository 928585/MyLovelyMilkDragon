package io.github.ninetwo.mdragon;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public class ModPotions {
    // ---- 1. 状态效果：奶龙之气 ----
    // 名字由用户 2026-10-02 定稿（此前是阶段 0/1 的旧占位名「龙息」dragon_breath，
    // 那个 id 还跟原版物品 minecraft:dragon_breath 撞名，已一并改掉）。
    // MobEffect 的构造函数是 protected，所以要写成匿名子类；
    // ⚠️ 空壳效果什么都不会发生 —— 目前它只显示一个图标，没有任何实际作用，
    // 想让效果真正生效还需要覆写 applyEffectTick / shouldApplyEffectTickThisTick
    // 或 addAttributeModifier，行为待用户明确后再加。
    public static final Holder<MobEffect> MILK_DRAGON_QI_EFFECT = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Mylovelymilkdragon.id("milk_dragon_qi"),
            new MobEffect(MobEffectCategory.BENEFICIAL, 0x6A0DAD) {} // 紫色，有益效果
    );

    // ---- 2. 药水：奶龙药水 ----
    // 26.3 起 Potion 的第一个参数是基础名，用于拼接翻译键：
    //   item.minecraft.potion.effect.<名字>
    // 注意这个键走的是【原版】命名空间，所以名字不能和原版药水重名。
    public static final ResourceKey<Potion> MILK_DRAGON_POTION_KEY = ResourceKey.create(
            Registries.POTION, Mylovelymilkdragon.id("milk_dragon"));

    public static final Holder<Potion> MILK_DRAGON_POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            MILK_DRAGON_POTION_KEY,
            new Potion("milk_dragon",
                    new MobEffectInstance(MILK_DRAGON_QI_EFFECT, 3600, 0)) // 持续 3 分钟
    );

    public static void registerModPotions() {
        // 空方法，触发类加载
    }
}
