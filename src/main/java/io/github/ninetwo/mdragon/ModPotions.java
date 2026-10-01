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
    // ---- 1. 状态效果 ----
    // MobEffect 的构造函数是 protected，所以要写成匿名子类；
    // 空壳效果什么都不会发生，想让效果真正生效还需要覆写
    // applyEffectTick / shouldApplyEffectTickThisTick 等方法
    public static final Holder<MobEffect> DRAGON_BREATH_EFFECT = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Mylovelymilkdragon.id("dragon_breath"),
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
                    new MobEffectInstance(DRAGON_BREATH_EFFECT, 3600, 0)) // 持续 3 分钟
    );

    public static void registerModPotions() {
        // 空方法，触发类加载
    }
}
